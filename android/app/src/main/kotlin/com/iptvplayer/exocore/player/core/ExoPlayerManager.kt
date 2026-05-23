package com.iptvplayer.exocore.player.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
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
import java.util.concurrent.Executors

class ExoPlayerManager(
    private val context: Context,
    private val onStateChange: (PlayerStateData) -> Unit,
    private val onDebugInfo: (Map<String, Any>) -> Unit,
    private val onError: (String) -> Unit,
) {
    private var player: ExoPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    // Background thread for network sniffing
    private val ioExecutor = Executors.newSingleThreadExecutor()

    private val stateManager = PlayerStateManager()
    private val headerManager = HttpHeaderManager()
    private val bandwidthMeterWrapper = BandwidthMeterWrapper()
    private val debugCollector = DebugInfoCollector(bandwidthMeterWrapper)
    private val dropMonitor = DropFrameMonitor()

    private var eventListener: PlayerEventListener? = null
    private var trackSelectorWrapper: TrackSelector? = null
    private var currentStream: ResolvedStream? = null

    private var debugRunnable: Runnable? = null
    private val DEBUG_INTERVAL_MS = 1000L

    private val retryManager = RetryManager(
        maxRetries = 3,
        retryDelayMs = 3000L,
        onRetry = { _ ->
            currentStream?.let { stream ->
                player?.stop()
                player?.clearMediaItems()
                loadStream(stream)
            }
        }
    )

    // ─── Public API ───────────────────────────────────────────────────────────

    fun play(url: String, typeHint: String, headers: Map<String, String>) {
        // State: initializing immediately
        mainHandler.post {
            stateManager.update { copy(state = ExoPlayerState.INITIALIZING) }
            onStateChange(stateManager.current)
            initOrResetPlayer()
        }

        // Sniff on IO thread (network call)
        ioExecutor.execute {
            val stream = StreamUrlResolver.resolve(url, typeHint, headers)
            currentStream = stream

            // Load on main thread
            mainHandler.post {
                loadStream(stream)
            }
        }
    }

    fun pause()  = mainHandler.post { player?.pause() }
    fun resume() = mainHandler.post { player?.play() }
    fun stop()   = mainHandler.post { player?.stop() }

    fun seekTo(positionMs: Long) = mainHandler.post {
        player?.seekTo(positionMs)
    }

    fun setVolume(volume: Float) = mainHandler.post {
        player?.volume = volume
    }

    fun retry() {
        mainHandler.post {
            stateManager.update { copy(state = ExoPlayerState.INITIALIZING) }
            onStateChange(stateManager.current)
        }
        val stream = currentStream
        if (stream != null) {
            ioExecutor.execute {
                val fresh = StreamUrlResolver.resolve(stream.url, "direct", stream.headers)
                currentStream = fresh
                mainHandler.post {
                    player?.stop()
                    player?.clearMediaItems()
                    loadStream(fresh)
                }
            }
        }
    }

    fun getStateMap(): Map<String, Any?> {
        val p = player ?: return stateManager.current.toMap()
        return stateManager.update {
            copy(
                positionMs   = p.currentPosition,
                durationMs   = p.duration.coerceAtLeast(0L),
                bufferPercent = p.bufferedPercentage,
            )
        }.toMap()
    }

    fun getDebugMap(): Map<String, Any> {
        val p = player ?: return DebugInfoCollector(BandwidthMeterWrapper()).collect(
            ExoPlayer.Builder(context).build().also { it.release() }
        )
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
            retryManager.destroy()
            eventListener?.let { player?.removeListener(it) }
            player?.removeAnalyticsListener(debugCollector.analyticsListener)
            player?.release()
            player = null
            stateManager.reset()
            debugCollector.reset()
            dropMonitor.reset()
        }
        ioExecutor.shutdown()
    }

    // ─── Private ──────────────────────────────────────────────────────────────

    private fun initOrResetPlayer() {
        stopDebugPolling()
        eventListener?.let { player?.removeListener(it) }
        player?.removeAnalyticsListener(debugCollector.analyticsListener)
        player?.release()
        player = null
        stateManager.reset()
        debugCollector.reset()
        dropMonitor.reset()
        buildPlayer()
    }

    private fun buildPlayer() {
        val bandwidthMeter = DefaultBandwidthMeter.Builder(context).build()
        bandwidthMeterWrapper.attach(bandwidthMeter)

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(5_000, 30_000, 2_000, 3_000)
            .build()

        val okHttpClient = headerManager.buildOkHttpClient()
        val okHttpFactory = OkHttpDataSource.Factory(okHttpClient)
        val dataSourceFactory = DefaultDataSource.Factory(context, okHttpFactory)

        val newPlayer = ExoPlayer.Builder(context)
            .setBandwidthMeter(bandwidthMeter)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .also { p ->
                p.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(),
                    true
                )
                p.playWhenReady = true
            }

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

        val dataSourceFactory = buildDataSourceFactory(stream.headers)

        val mediaSource: MediaSource = when (stream.type) {
            StreamType.HLS, StreamType.XTREAM_HLS ->
                HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)

            StreamType.TS, StreamType.XTREAM_TS ->
                ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)

            // DIRECT, XTREAM, UNKNOWN — ExoPlayer নিজে sniff করবে
            else ->
                DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(mediaItem)
        }

        p.setMediaSource(mediaSource)
        p.prepare()
    }

    private fun buildDataSourceFactory(headers: Map<String, String>): DefaultDataSource.Factory {
        val hm = HttpHeaderManager(headers)
        val okHttp = OkHttpDataSource.Factory(hm.buildOkHttpClient())
        return DefaultDataSource.Factory(context, okHttp)
    }

    private fun startDebugPolling() {
        stopDebugPolling()
        debugRunnable = object : Runnable {
            override fun run() {
                val p = player ?: return
                if (p.isPlaying || p.isLoading) onDebugInfo(debugCollector.collect(p))
                mainHandler.postDelayed(this, DEBUG_INTERVAL_MS)
            }
        }
        mainHandler.postDelayed(debugRunnable!!, DEBUG_INTERVAL_MS)
    }

    private fun stopDebugPolling() {
        debugRunnable?.let { mainHandler.removeCallbacks(it) }
        debugRunnable = null
    }
}
