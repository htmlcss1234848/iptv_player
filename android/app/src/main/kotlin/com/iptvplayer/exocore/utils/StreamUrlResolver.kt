package com.iptvplayer.exocore.utils

import androidx.media3.common.MediaItem

enum class StreamType {
    HLS, TS, XTREAM, XTREAM_HLS, XTREAM_TS, DIRECT, UNKNOWN
}

data class ResolvedStream(
    val url: String,
    val type: StreamType = StreamType.DIRECT,
    val headers: Map<String, String> = emptyMap(),
    val mimeType: String? = null, // null = ExoPlayer auto detect
)

object StreamUrlResolver {

    /**
     * ExoPlayer Media3 নিজেই redirect, HLS, TS, DASH সব handle করে।
     * mimeType null রাখলে ExoPlayer content sniff করে নেয়।
     * আমাদের কাজ শুধু URL + headers pass করা।
     */
    fun resolve(
        url: String,
        typeHint: String = "direct",
        headers: Map<String, String> = emptyMap(),
    ): ResolvedStream {
        return ResolvedStream(
            url = url,
            type = StreamType.DIRECT,
            headers = headers,
            mimeType = null, // ExoPlayer auto detect করবে
        )
    }

    fun buildMediaItem(stream: ResolvedStream): MediaItem {
        return MediaItem.Builder()
            .setUri(stream.url)
            // mimeType set করা হচ্ছে না — ExoPlayer নিজে sniff করবে
            .build()
    }
}
