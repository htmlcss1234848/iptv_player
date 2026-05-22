package com.iptvplayer.exocore.player.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.smoothstreaming.SsMediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.ui.PlayerView
import com.iptvplayer.exocore.player.debug.DebugInfoCollector
import com.iptvplayer.exocore.player.debug.DropFrameMonitor
import com.iptvplayer.exocore.player.network.BandwidthMeterWrapper
import com.iptvplayer.exocore.player.network.HttpHeaderManager
import com.iptvplayer.exocore.player.network.RetryManager
import com.iptvplayer.exocore.player.track.TrackSelector
import com.iptvplayer.exocore.utils.ResolvedStream
import com.iptvplayer.exocore.utils.StreamType
import com.iptvplayer.exocore.utils.StreamUrlResolver

class ExoPlayerManager(
    private val context: Context,
    private val onStateChange: (PlayerStateData) -> Unit,
    private val onDebugInfo: (Map<String, Any>) -> Unit,
    private val onError: (String) -> Unit,
) {
    private var player: ExoPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val stateManager = PlayerStateManager()
    private val headerManager = HttpHeaderManager()
    private val bandwidthMeterWrapper = BandwidthMeterWrapper()
    private val debugCollector = DebugInfoCollector(bandwidthMeterWrapper)
    private val dropMonitor = DropFrameMonitor()

    private var eventListener: PlayerEventListener? = null
    private var trackSelectorWrapper: TrackSelector? = null
    private var currentStream: ResolvedStream? = null

    // Debug polling timer
    private var debugRunnable: Runnable? = null
    private val DEBUG_INTERVAL_MS = 1000L

    // ─── Public API ───────────────────────────────────────────────────────────

    fun play(
        url: String,
        typeHint: String,
        headers: Map<String, String>,
    ) {
        mainHandler.post {
            val stream = StreamUrlResolver.resolve(url, typeHint, headers)
            currentStream = stream
            headerManager.updateHeaders(headers)
            initOrResetPlayer()
            loadStream(stream)
        }
    }

    fun pause() = mainHandler.post { player?.pause() }
    fun resume() = mainHandler.post { player?.play() }
    fun stop() = mainHandler.post { player?.stop() }

    fun seekTo(positionMs: Long) = mainHandler.post {
        player?.seekTo(positionMs)
    }

    fun setVolume(volume: Float) = mainHandler.post {
        player?.volume = volume
    }

    fun retry() = mainHandler.post {
        currentStream?.let { stream ->
            player?.stop()
            player?.clearMediaItems()
            loadStream(stream)
        }
    }

    fun getStateMap(): Map<String, Any?> {
        val p = player ?: return stateManager.current.toMap()
        return stateManager.update {
            copy(
                positionMs = p.currentPosition,
                durationMs = p.duration.coerceAtLeast(0L),
                bufferPercent = p.bufferedPercentage,
            )
        }.toMap()
    }

    fun getDebugMap(): Map<String, Any> {
        val p = player ?: return debugCollector.collect(buildDummyPlayer())
        return debugCollector.collect(p)
    }

    fun attachView(playerView: PlayerView) {
        mainHandler.post { playerView.player = player }
    }

    fun detachView(playerView: PlayerView) {
        mainHandler.post { playerView.player = null }
    }

    fun dispose() {
        mainHandler.post {
            stopDebugPolling()
            eventListener?.let { player?.removeListener(it) }
            player?.removeAnalyticsListener(debugCollector.analyticsListener)
            player?.release()
            player = null
            stateManager.reset()
            debugCollector.reset()
            dropMonitor.reset()
        }
    }

    // ─── Private: Player init ─────────────────────────────────────────────────

    private fun initOrResetPlayer() {
        if (player != null) {
            stopDebugPolling()
            eventListener?.let { player?.removeListener(it) }
            player?.removeAnalyticsListener(debugCollector.analyticsListener)
            player?.release()
            player = null
        }
        stateManager.reset()
        debugCollector.reset()
        dropMonitor.reset()
        buildPlayer()
    }

    private fun buildPlayer() {
        val bandwidthMeter = DefaultBandwidthMeter.Builder(context).build()
        bandwidthMeterWrapper.attach(bandwidthMeter)

        val okHttpClient = headerManager.buildOkHttpClient()
        val okHttpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        val dataSourceFactory = DefaultDataSource.Factory(context, okHttpDataSourceFactory)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs= */        5_000,
                /* maxBufferMs= */        30_000,
                /* bufferForPlaybackMs= */ 2_000,
                /* bufferForPlaybackAfterRebufferMs= */ 3_000
            )
            .build()

        val newPlayer = ExoPlayer.Builder(context)
            .setBandwidthMeter(bandwidthMeter)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .also { p ->
                // Audio focus
                p.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    /* handleAudioFocus= */ true
                )
                p.playWhenReady = true
            }

        // Attach listeners
        val listener = PlayerEventListener(
            stateManager = stateManager,
            onStateChange = { state ->
                onStateChange(state)
                if (state.state == ExoPlayerState.ERROR && retryManager.hasRetriesLeft) {
                    retryManager.onError()
                }
            },
            onError = onError,
        )
        newPlayer.addListener(listener)
        newPlayer.addAnalyticsListener(debugCollector.analyticsListener)

        eventListener = listener
        player = newPlayer
        trackSelectorWrapper = TrackSelector(newPlayer)

        startDebugPolling()
    }

    private fun loadStream(stream: ResolvedStream) {
        val p = player ?: return
        val mediaItem = StreamUrlResolver.buildMediaItem(stream)

        val mediaSource: MediaSource = when (stream.type) {
            StreamType.HLS, StreamType.XTREAM_HLS -> {
                val dataSourceFactory = buildDataSourceFactory(stream.headers)
                HlsMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
            StreamType.TS, StreamType.XTREAM_TS -> {
                val dataSourceFactory = buildDataSourceFactory(stream.headers)
                ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
            StreamType.XTREAM, StreamType.DIRECT -> {
                // Let ExoPlayer sniff the format automatically
                val dataSourceFactory = buildDataSourceFactory(stream.headers)
                DefaultMediaSourceFactory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
            StreamType.UNKNOWN -> {
                val dataSourceFactory = buildDataSourceFactory(stream.headers)
                DefaultMediaSourceFactory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            }
        }

        p.setMediaSource(mediaSource)
        p.prepare()

        stateManager.update { copy(state = ExoPlayerState.INITIALIZING) }
        onStateChange(stateManager.current)
    }

    private fun buildDataSourceFactory(
        headers: Map<String, String>
    ): DefaultDataSource.Factory {
        val hm = HttpHeaderManager(headers)
        val okHttpClient = hm.buildOkHttpClient()
        val okHttpFactory = OkHttpDataSource.Factory(okHttpClient)
        return DefaultDataSource.Factory(context, okHttpFactory)
    }

    // ─── Debug polling ────────────────────────────────────────────────────────

    private fun startDebugPolling() {
        stopDebugPolling()
        debugRunnable = object : Runnable {
            override fun run() {
                val p = player ?: return
                if (p.isPlaying || p.isLoading) {
                    onDebugInfo(debugCollector.collect(p))
                }
                mainHandler.postDelayed(this, DEBUG_INTERVAL_MS)
            }
        }
        mainHandler.postDelayed(debugRunnable!!, DEBUG_INTERVAL_MS)
    }

    private fun stopDebugPolling() {
        debugRunnable?.let { mainHandler.removeCallbacks(it) }
        debugRunnable = null
    }

    // ─── Retry ────────────────────────────────────────────────────────────────

    private val retryManager = RetryManager(
        maxRetries = 3,
        retryDelayMs = 3000L,
        onRetry = { attempt ->
            currentStream?.let { stream ->
                player?.stop()
                player?.clearMediaItems()
                loadStream(stream)
            }
        }
    )

    // ─── Dummy player for safe debug calls ───────────────────────────────────

    private fun buildDummyPlayer(): ExoPlayer {
        return player ?: ExoPlayer.Builder(context).build().also { player = it }
    }
}
