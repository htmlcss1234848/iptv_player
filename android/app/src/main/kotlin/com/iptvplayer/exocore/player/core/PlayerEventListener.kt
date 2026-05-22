package com.iptvplayer.exocore.player.core

import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize

class PlayerEventListener(
    private val stateManager: PlayerStateManager,
    private val onStateChange: (PlayerStateData) -> Unit,
    private val onError: (String) -> Unit,
) : Player.Listener {

    override fun onPlaybackStateChanged(playbackState: Int) {
        val newState = stateManager.update {
            when (playbackState) {
                Player.STATE_IDLE      -> copy(state = ExoPlayerState.IDLE)
                Player.STATE_BUFFERING -> copy(state = ExoPlayerState.BUFFERING)
                Player.STATE_READY     -> copy(state = ExoPlayerState.READY)
                Player.STATE_ENDED     -> copy(state = ExoPlayerState.ENDED)
                else                   -> this
            }
        }
        onStateChange(newState)
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        val newState = stateManager.update {
            if (isPlaying) copy(state = ExoPlayerState.PLAYING)
            else if (state == ExoPlayerState.PLAYING) copy(state = ExoPlayerState.PAUSED)
            else this
        }
        onStateChange(newState)
    }

    override fun onPlayerError(error: PlaybackException) {
        val msg = buildErrorMessage(error)
        stateManager.update {
            copy(state = ExoPlayerState.ERROR, errorMessage = msg)
        }
        onError(msg)
    }

    override fun onVideoSizeChanged(videoSize: VideoSize) {
        // VideoSize changes are captured in DebugInfoCollector
    }

    private fun buildErrorMessage(error: PlaybackException): String {
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED ->
                "Network connection failed. Check your internet."
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                "Connection timed out. Server may be down."
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                "Server returned bad HTTP status. URL may be invalid."
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED ->
                "Malformed stream. Cannot parse container."
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                "Malformed HLS/DASH manifest."
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
                "Decoder initialization failed."
            else -> "Playback error (${error.errorCode}): ${error.message}"
        }
    }
}
