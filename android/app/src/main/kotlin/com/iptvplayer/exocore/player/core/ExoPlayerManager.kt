package com.iptvplayer.exocore.player.core

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.ui.PlayerView
import com.iptvplayer.exocore.player.debug.DebugInfoCollector
import com.iptvplayer.exocore.player.debug.DropFrameMonitor
import com.iptvplayer.exocore.player.network.BandwidthMeterWrapper
import com.iptvplayer.exocore.player.network.HttpHeaderManager
import com.iptvplayer.exocore.player.track.TrackSelector
import com.iptvplayer.exocore.utils.ResolvedStream
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
    private val ioExecutor = Executors.newSingleThreadExecutor()

    private val stateManager = PlayerStateManager()
    private val bandwidthMeterWrapper = BandwidthMeterWrapper()
    private val debugCollector = DebugInfoCollector(bandwidthMeterWrapper)
    private val dropMonitor = DropFrameMonitor()

    private var debugRunnable: Runnable? = null
    private val DEBUG_INTERVAL_MS = 1000L

    // ─── Resolution state ─────────────────────────────────────────────────────
    private val MAX_DEPTH = 3
    private val STUCK_TIMEOUT_MS = 9000L  // 9s — not playing → resolve

    private var currentDepth = 0
    private val visitedUrls = mutableSetOf<String>()
    private var currentStream: ResolvedStream? = null
    private var originalUrl = ""
    private var originalHeaders = emptyMap<String, String>()

    // Stuck detection timer
    private var stuckTimeoutRunnable: Runnable? = null
    // Flag: resolution already triggered for this layer
    private var resolvingTriggered = false

    // ─── Public API ───────────────────────────────────────────────────────────

    fun play(url: String, typeHint: String, headers: Map<String, String>) {
        mainHandler.post {
            originalUrl = url
            originalHeaders = headers
            currentDepth = 0
            visitedUrls.clear()
            stateManager.update { copy(state = ExoPlayerState.INITIALIZING) }
            onStateChange(stateManager.current)
            initOrResetPlayer()
            tryPlayLayer(ResolvedStream(url = url, headers = headers))
        }
    }

    fun pause()  = mainHandler.post { player?.pause() }
    fun resume() = mainHandler.post { player?.play() }
    fun stop()   = mainHandler.post { cancelStuckTimer(); player?.stop() }

    fun seekTo(positionMs: Long) = mainHandler.post { player?.seekTo(positionMs) }
    fun setVolume(volume: Float) = mainHandler.post { player?.volume = volume }

    fun retry() {
        mainHandler.post {
            currentDepth = 0
            visitedUrls.clear()
            cancelStuckTimer()
            stateManager.update { copy(state = ExoPlayerState.INITIALIZING) }
            onStateChange(stateManager.current)
            player?.stop()
            player?.clearMediaItems()
            tryPlayLayer(ResolvedStream(url = originalUrl, headers = originalHeaders))
        }
    }

    fun getStateMap(): Map<String, Any?> {
        val p = player ?: return stateManager.current.toMap()
        return stateManager.update {
            copy(
                positionMs    = p.currentPosition,
                durationMs    = p.duration.coerceAtLeast(0L),
                bufferPercent = p.bufferedPercentage,
            )
        }.toMap()
    }

    fun getDebugMap(): Map<String, Any> =
        player?.let { debugCollector.collect(it) } ?: emptyMap()

    fun attachView(playerView: PlayerView) =
        mainHandler.post { playerView.player = player }

    fun detachView(playerView: PlayerView) =
        mainHandler.post { playerView.player = null }

    fun dispose() {
        mainHandler.post {
            cancelStuckTimer()
            stopDebugPolling()
            player?.removeAnalyticsListener(debugCollector.analyticsListener)
            player?.release()
            player = null
            stateManager.reset()
            debugCollector.reset()
            visitedUrls.clear()
        }
        ioExecutor.shutdown()
    }

    // ─── Core: Try play at current layer ──────────────────────────────────────

    private fun tryPlayLayer(stream: ResolvedStream) {
        if (currentDepth > MAX_DEPTH) {
            reportFinalError("Could not play stream after $MAX_DEPTH resolution attempts.")
            return
        }
        if (visitedUrls.contains(stream.url)) {
            reportFinalError("Circular redirect detected.")
            return
        }

        visitedUrls.add(stream.url)
        currentStream = stream
        resolvingTriggered = false

        val p = player ?: return
        val mediaItem = StreamUrlResolver.buildMediaItem(stream)
        val mediaSource = DefaultMediaSourceFactory(
            buildDataSourceFactory(stream.headers)
        ).createMediaSource(mediaItem)

        p.stop()
        p.clearMediaItems()
        p.setMediaSource(mediaSource)
        p.prepare()

        // CASE 2: Start stuck timer — if no playback in STUCK_TIMEOUT_MS → resolve
        startStuckTimer(stream)
    }

    // ─── CASE 1: Immediate error → resolve now ────────────────────────────────

    private fun onImmediateError(error: PlaybackException) {
        cancelStuckTimer()
        if (resolvingTriggered) return
        resolvingTriggered = true
        resolveNextLayer(currentStream, reason = "ExoPlayer error: ${error.errorCode}")
    }

    // ─── CASE 2: Stuck timer — no playback after timeout → resolve ────────────

    private fun startStuckTimer(stream: ResolvedStream) {
        cancelStuckTimer()
        stuckTimeoutRunnable = Runnable {
            val p = player ?: return@Runnable
            val isPlayingOrReady = p.isPlaying ||
                    p.playbackState == Player.STATE_READY
            if (isPlayingOrReady) return@Runnable  // Playing fine → do nothing

            // Still not playing → resolve
            if (resolvingTriggered) return@Runnable
            resolvingTriggered = true
            resolveNextLayer(stream, reason = "Stuck: no playback after ${STUCK_TIMEOUT_MS}ms")
        }
        mainHandler.postDelayed(stuckTimeoutRunnable!!, STUCK_TIMEOUT_MS)
    }

    private fun cancelStuckTimer() {
        stuckTimeoutRunnable?.let { mainHandler.removeCallbacks(it) }
        stuckTimeoutRunnable = null
    }

    // ─── Resolve next layer ───────────────────────────────────────────────────

    private fun resolveNextLayer(stream: ResolvedStream?, reason: String) {
        if (stream == null) { reportFinalError("No stream to resolve."); return }

        currentDepth++
        if (currentDepth > MAX_DEPTH) {
            // MAX_DEPTH hit → Force HLS retry on original URL (Section 10, Step 4)
            forceHlsRetry()
            return
        }

        stateManager.update { copy(state = ExoPlayerState.INITIALIZING) }
        onStateChange(stateManager.current)

        ioExecutor.execute {
            val next = StreamUrlResolver.resolveNextLayer(
                url = stream.url,
                headers = stream.headers,
            )
            mainHandler.post {
                when {
                    // Got a new URL → try play
                    next != null && next.url != stream.url -> {
                        tryPlayLayer(next)
                    }
                    // No new URL → Force HLS on current URL
                    else -> forceHlsRetry()
                }
            }
        }
    }

    // ─── Force HLS retry (Section 10, Step 4) ────────────────────────────────

    private fun forceHlsRetry() {
        val stream = currentStream ?: run { reportFinalError("Stream unavailable."); return }

        // Force HLS mimeType on the same URL
        val hlsStream = stream.copy(
            mimeType = androidx.media3.common.MimeTypes.APPLICATION_M3U8
        )

        val p = player ?: return
        val mediaItem = androidx.media3.common.MediaItem.Builder()
            .setUri(hlsStream.url)
            .setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
            .build()
        val mediaSource = DefaultMediaSourceFactory(
            buildDataSourceFactory(hlsStream.headers)
        ).createMediaSource(mediaItem)

        p.stop()
        p.clearMediaItems()
        p.setMediaSource(mediaSource)
        p.prepare()

        // One last stuck timer — if HLS also fails → final error
        cancelStuckTimer()
        stuckTimeoutRunnable = Runnable {
            val isOk = player?.isPlaying == true ||
                    player?.playbackState == Player.STATE_READY
            if (!isOk) reportFinalError("Stream not playable even with forced HLS.")
        }
        mainHandler.postDelayed(stuckTimeoutRunnable!!, STUCK_TIMEOUT_MS)
    }

    private fun reportFinalError(msg: String) {
        cancelStuckTimer()
        stateManager.update { copy(state = ExoPlayerState.ERROR, errorMessage = msg) }
        onStateChange(stateManager.current)
        onError(msg)
    }

    // ─── Player init ──────────────────────────────────────────────────────────

    private fun initOrResetPlayer() {
        cancelStuckTimer()
        stopDebugPolling()
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

        val headerManager = HttpHeaderManager()
        val dataSourceFactory = DefaultDataSource.Factory(
            context, OkHttpDataSource.Factory(headerManager.buildOkHttpClient())
        )

        val newPlayer = ExoPlayer.Builder(context)
            .setBandwidthMeter(bandwidthMeter)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build().also { p ->
                p.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                        .build(), true
                )
                p.playWhenReady = true
            }

        newPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                val newState = stateManager.update {
                    when (state) {
                        Player.STATE_BUFFERING -> copy(state = ExoPlayerState.BUFFERING)
                        Player.STATE_READY     -> copy(state = ExoPlayerState.READY)
                        Player.STATE_ENDED     -> copy(state = ExoPlayerState.ENDED)
                        Player.STATE_IDLE      -> this
                        else                   -> this
                    }
                }
                onStateChange(newState)
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    // SUCCESS → cancel everything, stop resolving
                    cancelStuckTimer()
                    resolvingTriggered = false
                }
                val newState = stateManager.update {
                    if (isPlaying) copy(state = ExoPlayerState.PLAYING)
                    else if (state == ExoPlayerState.PLAYING) copy(state = ExoPlayerState.PAUSED)
                    else this
                }
                onStateChange(newState)
            }

            override fun onPlayerError(error: PlaybackException) {
                // CASE 1: Immediate error → resolve now
                onImmediateError(error)
            }
        })

        newPlayer.addAnalyticsListener(debugCollector.analyticsListener)
        player = newPlayer
        TrackSelector(newPlayer)
        startDebugPolling()
    }

    private fun buildDataSourceFactory(
        headers: Map<String, String>
    ): DefaultDataSource.Factory {
        val hm = HttpHeaderManager(headers)
        return DefaultDataSource.Factory(context, OkHttpDataSource.Factory(hm.buildOkHttpClient()))
    }

    // ─── Debug polling ────────────────────────────────────────────────────────

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
