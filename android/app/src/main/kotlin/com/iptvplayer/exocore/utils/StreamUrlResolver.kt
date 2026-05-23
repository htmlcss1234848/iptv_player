package com.iptvplayer.exocore.utils

import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

enum class StreamType {
    HLS, TS, XTREAM, XTREAM_HLS, XTREAM_TS, DIRECT, UNKNOWN
}

data class ResolvedStream(
    val url: String,
    val type: StreamType,
    val headers: Map<String, String> = emptyMap(),
    val mimeType: String? = null,
)

/**
 * Stream type detection — priority:
 *
 * 1. HEAD → Content-Type + byte sniff (actual truth)
 * 2. GET  → Content-Type + byte sniff (if HEAD fails/405)
 * 3. ExoPlayer sniff (no mimeType hint — smartest fallback)
 * 4. URL/query hint → retry with explicit type (last resort)
 *
 * Extension এবং query param (?extension=ts) কে প্রথমে trust করা হয় না।
 * কারণ server মিথ্যা extension দিতে পারে।
 */
object StreamUrlResolver {

    private val sniffClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun resolve(
        url: String,
        typeHint: String = "direct",
        headers: Map<String, String> = emptyMap(),
    ): ResolvedStream {
        // Step 1 & 2: Actual content check (HEAD → GET)
        val sniffed = sniffActualType(url, headers)
        if (sniffed != null) return sniffed

        // Step 3: ExoPlayer sniff — no mimeType, let ExoPlayer figure it out
        // This handles redirects, adaptive streams, anything Media3 supports
        return ResolvedStream(url, StreamType.DIRECT, headers, null)
    }

    // ─── Sniff: HEAD then GET ─────────────────────────────────────────────────

    private fun sniffActualType(
        url: String,
        headers: Map<String, String>,
    ): ResolvedStream? {
        // HEAD first
        val headResult = tryRequest(url, headers, method = "HEAD")
        if (headResult != null) return headResult

        // GET fallback
        return tryRequest(url, headers, method = "GET")
    }

    private fun tryRequest(
        url: String,
        headers: Map<String, String>,
        method: String,
    ): ResolvedStream? {
        return try {
            val req = buildRequest(url, headers, method)
            sniffClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful && resp.code in listOf(405, 501)) return null

                val contentType = resp.header("Content-Type")?.lowercase()
                val finalUrl    = resp.request.url.toString()
                val bodyBytes   = if (method == "GET") resp.body?.bytes()?.take(188) else null

                // Content-Type থেকে detect
                val fromCT = contentType?.let { detectFromContentType(finalUrl, it, headers) }
                if (fromCT != null) return fromCT

                // Body bytes থেকে detect (GET only)
                if (bodyBytes != null) {
                    val fromBytes = detectFromBytes(finalUrl, bodyBytes, headers)
                    if (fromBytes != null) return fromBytes
                }

                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // ─── Content-Type detection ───────────────────────────────────────────────

    private fun detectFromContentType(
        url: String,
        ct: String,
        headers: Map<String, String>,
    ): ResolvedStream? = when {
        ct.contains("mpegurl") ||
        ct.contains("x-mpegurl") ||
        ct.contains("m3u8") ->
            ResolvedStream(url, StreamType.HLS, headers, MimeTypes.APPLICATION_M3U8)

        ct.contains("mp2t") ||
        ct.contains("mpeg2ts") ->
            ResolvedStream(url, StreamType.TS, headers, MimeTypes.VIDEO_MP2T)

        ct.contains("mp4") ->
            ResolvedStream(url, StreamType.DIRECT, headers, MimeTypes.VIDEO_MP4)

        ct.contains("dash") ||
        ct.contains("mpd") ->
            ResolvedStream(url, StreamType.DIRECT, headers, MimeTypes.APPLICATION_MPD)

        // octet-stream — bytes দেখতে হবে
        ct.contains("octet-stream") -> null

        // text/plain — হয়তো M3U8 playlist
        ct.contains("text/plain") -> null

        else -> null
    }

    // ─── Byte-level detection ─────────────────────────────────────────────────

    private fun detectFromBytes(
        url: String,
        bytes: List<Byte>,
        headers: Map<String, String>,
    ): ResolvedStream? {
        if (bytes.isEmpty()) return null

        // TS: sync byte 0x47 at position 0
        if (bytes[0] == 0x47.toByte()) {
            return ResolvedStream(url, StreamType.TS, headers, MimeTypes.VIDEO_MP2T)
        }

        // M3U8 playlist: starts with #EXTM3U or #EXT-X-
        val text = bytes.map { it.toInt().toChar() }.joinToString("").trimStart()
        if (text.startsWith("#EXTM3U") || text.startsWith("#EXT-X-")) {
            return ResolvedStream(url, StreamType.HLS, headers, MimeTypes.APPLICATION_M3U8)
        }

        // MP4: ftyp/moov/mdat box at byte 4
        if (bytes.size >= 8) {
            val box = bytes.subList(4, 8).map { it.toInt().toChar() }.joinToString("")
            if (box in listOf("ftyp", "moov", "mdat", "free")) {
                return ResolvedStream(url, StreamType.DIRECT, headers, MimeTypes.VIDEO_MP4)
            }
        }

        // MPEG-PS: starts with 0x000001BA
        if (bytes.size >= 4 &&
            bytes[0] == 0x00.toByte() && bytes[1] == 0x00.toByte() &&
            bytes[2] == 0x01.toByte() && bytes[3] == 0xBA.toByte()) {
            return ResolvedStream(url, StreamType.DIRECT, headers, null)
        }

        return null
    }

    // ─── Request builder ──────────────────────────────────────────────────────

    private fun buildRequest(
        url: String,
        headers: Map<String, String>,
        method: String,
    ): Request {
        val builder = Request.Builder().url(url)
        headers.forEach { (k, v) -> builder.header(k, v) }
        if (builder.build().header("User-Agent") == null) {
            builder.header("User-Agent",
                "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 " +
                "Chrome/91.0.4472.120 Mobile Safari/537.36")
        }
        return when (method) {
            "HEAD" -> builder.head().build()
            else   -> builder.get().build()
        }
    }

    // ─── Media Item builder ───────────────────────────────────────────────────

    fun buildMediaItem(stream: ResolvedStream): MediaItem {
        val builder = MediaItem.Builder().setUri(stream.url)
        if (stream.mimeType != null) builder.setMimeType(stream.mimeType)
        return builder.build()
    }
}
