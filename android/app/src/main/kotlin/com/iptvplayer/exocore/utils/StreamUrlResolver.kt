package com.iptvplayer.exocore.utils

import androidx.media3.common.MediaItem
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

enum class StreamType { DIRECT, UNKNOWN }

data class ResolvedStream(
    val url: String,
    val type: StreamType = StreamType.DIRECT,
    val headers: Map<String, String> = emptyMap(),
    val mimeType: String? = null,
)

/**
 * URL resolver — only called AFTER ExoPlayer fails.
 * Follows redirects and returns the next layer URL.
 */
object StreamUrlResolver {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    /**
     * Try to get the next layer URL via HEAD then GET.
     * Returns null if no new URL found.
     */
    fun resolveNextLayer(
        url: String,
        headers: Map<String, String> = emptyMap(),
    ): ResolvedStream? {
        // HEAD first
        val headUrl = tryHead(url, headers)
        if (headUrl != null && headUrl != url) {
            return ResolvedStream(url = headUrl, headers = headers)
        }

        // GET — body তে URL থাকতে পারে (e.g. plain text redirect)
        return tryGet(url, headers)
    }

    private fun tryHead(url: String, headers: Map<String, String>): String? {
        return try {
            val req = buildRequest(url, headers, "HEAD")
            client.newCall(req).execute().use { resp ->
                val finalUrl = resp.request.url.toString()
                if (finalUrl != url) finalUrl else null
            }
        } catch (e: Exception) { null }
    }

    private fun tryGet(url: String, headers: Map<String, String>): ResolvedStream? {
        return try {
            val req = buildRequest(url, headers, "GET")
            client.newCall(req).execute().use { resp ->
                val finalUrl = resp.request.url.toString()
                val body = resp.body?.string()?.trim() ?: ""

                // Body তে single URL আছে? (plain text redirect)
                if (body.startsWith("http") && !body.contains("\n") && body.length < 2048) {
                    return ResolvedStream(url = body, headers = headers)
                }

                // Redirect হয়েছে
                if (finalUrl != url) {
                    return ResolvedStream(url = finalUrl, headers = headers)
                }

                null
            }
        } catch (e: Exception) { null }
    }

    private fun buildRequest(url: String, headers: Map<String, String>, method: String): Request {
        val builder = Request.Builder().url(url)
        headers.forEach { (k, v) -> builder.header(k, v) }
        if (builder.build().header("User-Agent") == null) {
            builder.header("User-Agent",
                "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 Chrome/91.0 Mobile Safari/537.36")
        }
        return if (method == "HEAD") builder.head().build() else builder.get().build()
    }

    fun buildMediaItem(stream: ResolvedStream): MediaItem =
        MediaItem.Builder().setUri(stream.url).build()
}

