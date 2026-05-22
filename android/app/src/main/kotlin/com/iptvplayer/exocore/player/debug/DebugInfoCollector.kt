package com.iptvplayer.exocore.player.debug

import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.common.Format
import androidx.media3.common.VideoSize
import com.iptvplayer.exocore.player.network.BandwidthMeterWrapper

class DebugInfoCollector(
    private val bandwidthMeter: BandwidthMeterWrapper,
) {
    // Video
    private var videoCodec: String = "-"
    private var videoWidth: Int = 0
    private var videoHeight: Int = 0
    private var videoFrameRate: Float = 0f
    private var videoBitrate: Int = 0

    // Audio
    private var audioCodec: String = "-"
    private var audioBitrate: Int = 0

    // Dropped frames
    private var totalDroppedFrames: Int = 0

    val analyticsListener = object : AnalyticsListener {

        override fun onVideoInputFormatChanged(
            eventTime: AnalyticsListener.EventTime,
            format: Format,
            decoderReuseEvaluation: androidx.media3.exoplayer.DecoderReuseEvaluation?
        ) {
            videoCodec = format.codecs?.substringBefore(".")?.uppercase() ?: "-"
            videoWidth = format.width
            videoHeight = format.height
            videoFrameRate = format.frameRate
            videoBitrate = format.bitrate.coerceAtLeast(0)
        }

        override fun onAudioInputFormatChanged(
            eventTime: AnalyticsListener.EventTime,
            format: Format,
            decoderReuseEvaluation: androidx.media3.exoplayer.DecoderReuseEvaluation?
        ) {
            audioCodec = format.codecs?.substringBefore(".")?.uppercase() ?: "-"
            audioBitrate = format.bitrate.coerceAtLeast(0)
        }

        override fun onDroppedVideoFrames(
            eventTime: AnalyticsListener.EventTime,
            droppedFrames: Int,
            elapsedMs: Long
        ) {
            totalDroppedFrames += droppedFrames
        }

        override fun onVideoSizeChanged(
            eventTime: AnalyticsListener.EventTime,
            videoSize: VideoSize
        ) {
            videoWidth = videoSize.width
            videoHeight = videoSize.height
        }
    }

    fun collect(player: ExoPlayer): Map<String, Any> {
        val bufferMs = player.totalBufferedDuration

        return mapOf(
            "type"          to "debug",
            "videoCodec"    to videoCodec,
            "audioCodec"    to audioCodec,
            "videoBitrate"  to videoBitrate,
            "audioBitrate"  to audioBitrate,
            "totalBitrate"  to (videoBitrate + audioBitrate),
            "resolution"    to if (videoWidth > 0) "${videoWidth}x${videoHeight}" else "-",
            "fps"           to videoFrameRate.toDouble(),
            "droppedFrames" to totalDroppedFrames,
            "bufferMs"      to bufferMs.toInt(),
            "networkSpeed"  to bandwidthMeter.formattedSpeed,
            "rendererName"  to "ExoPlayer Media3",
            "videoWidth"    to videoWidth,
            "videoHeight"   to videoHeight,
        )
    }

    fun reset() {
        videoCodec = "-"
        audioCodec = "-"
        videoWidth = 0
        videoHeight = 0
        videoFrameRate = 0f
        videoBitrate = 0
        audioBitrate = 0
        totalDroppedFrames = 0
    }
}
