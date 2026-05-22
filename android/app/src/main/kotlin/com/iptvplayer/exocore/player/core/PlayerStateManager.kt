package com.iptvplayer.exocore.player.core

enum class ExoPlayerState {
    IDLE,
    INITIALIZING,
    BUFFERING,
    READY,
    PLAYING,
    PAUSED,
    ENDED,
    ERROR;

    fun toFlutterString(): String = name.lowercase()
}

data class PlayerStateData(
    val state: ExoPlayerState = ExoPlayerState.IDLE,
    val errorMessage: String? = null,
    val bufferPercent: Int = 0,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "type"          to "state",
        "state"         to state.toFlutterString(),
        "error"         to errorMessage,
        "bufferPercent" to bufferPercent,
        "positionMs"    to positionMs.toInt(),
        "durationMs"    to durationMs.toInt(),
    )
}

class PlayerStateManager {
    private var _state = PlayerStateData()
    val current get() = _state

    fun update(block: PlayerStateData.() -> PlayerStateData): PlayerStateData {
        _state = _state.block()
        return _state
    }

    fun reset() {
        _state = PlayerStateData()
    }
}
