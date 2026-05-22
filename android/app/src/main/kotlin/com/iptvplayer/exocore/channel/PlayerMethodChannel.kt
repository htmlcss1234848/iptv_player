package com.iptvplayer.exocore.channel

import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import com.iptvplayer.exocore.player.core.ExoPlayerManager

/**
 * Receives method calls from Flutter and delegates to ExoPlayerManager.
 */
class PlayerMethodChannel(
    messenger: BinaryMessenger,
    private val playerManagerProvider: () -> ExoPlayerManager?,
) : MethodChannel.MethodCallHandler {

    private val methodChannel = MethodChannel(messenger, "com.iptvplayer/exoplayer")

    init {
        methodChannel.setMethodCallHandler(this)
    }

    @Suppress("UNCHECKED_CAST")
    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        val manager = playerManagerProvider()

        when (call.method) {
            "play" -> {
                val url     = call.argument<String>("url") ?: ""
                val type    = call.argument<String>("type") ?: "direct"
                val headers = (call.argument<Map<*, *>>("headers") ?: emptyMap<Any, Any>())
                    .entries.associate { it.key.toString() to it.value.toString() }

                manager?.play(url, type, headers)
                result.success(null)
            }
            "pause"  -> { manager?.pause();  result.success(null) }
            "resume" -> { manager?.resume(); result.success(null) }
            "stop"   -> { manager?.stop();   result.success(null) }
            "retry"  -> { manager?.retry();  result.success(null) }
            "dispose" -> { manager?.dispose(); result.success(null) }

            "seekTo" -> {
                val ms = call.argument<Int>("positionMs")?.toLong() ?: 0L
                manager?.seekTo(ms)
                result.success(null)
            }

            "setVolume" -> {
                val volume = (call.argument<Double>("volume") ?: 1.0).toFloat()
                manager?.setVolume(volume)
                result.success(null)
            }

            "getState" -> {
                result.success(manager?.getStateMap() ?: emptyMap<String, Any>())
            }

            "getDebugInfo" -> {
                result.success(manager?.getDebugMap() ?: emptyMap<String, Any>())
            }

            else -> result.notImplemented()
        }
    }

    fun dispose() {
        methodChannel.setMethodCallHandler(null)
    }
}
