package com.iptvplayer.exocore.utils

import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes

enum class StreamType {
    HLS, TS, XTREAM, XTREAM_HLS, XTREAM_TS, DIRECT, UNKNOWN
}

data class ResolvedStream(
    val url: String,
    val type: StreamType,
    val headers: Map<String, String> = emptyMap(),
    val mimeType: String? = null,
)

object StreamUrlResolver {

    fun resolve(
        url: String,
        typeHint: String = "direct",
        headers: Map<String, String> = emptyMap()
    ): ResolvedStream {
        val lower = url.lowercase()

        return when {
            lower.contains(".m3u8") -> ResolvedStream(
                url = url,
                type = StreamType.HLS,
                headers = headers,
                mimeType = MimeTypes.APPLICATION_M3U8,
            )
            typeHint == "xtreamHls" -> ResolvedStream(
                url = url,
                type = StreamType.XTREAM_HLS,
                headers = headers,
                mimeType = MimeTypes.APPLICATION_M3U8,
            )
            typeHint == "xtreamTs" || lower.endsWith(".ts") -> ResolvedStream(
                url = url,
                type = StreamType.XTREAM_TS,
                headers = headers,
                mimeType = MimeTypes.VIDEO_MP2T,
            )
            typeHint == "xtream" -> ResolvedStream(
                url = url,
                type = StreamType.XTREAM,
                headers = headers,
                // No mimeType — let ExoPlayer sniff
            )
            else -> ResolvedStream(
                url = url,
                type = StreamType.DIRECT,
                headers = headers,
            )
        }
    }

    fun buildMediaItem(stream: ResolvedStream): MediaItem {
        val builder = MediaItem.Builder().setUri(stream.url)

        if (stream.mimeType != null) {
            builder.setMimeType(stream.mimeType)
        }

        if (stream.headers.isNotEmpty()) {
            val requestHeaders = stream.headers.toMutableMap()
            builder.setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setExtras(android.os.Bundle().apply {
                        // Headers passed via OkHttp interceptor, not here
                    })
                    .build()
            )
        }

        return builder.build()
    }
}
