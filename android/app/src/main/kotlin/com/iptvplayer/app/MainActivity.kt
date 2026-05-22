package com.iptvplayer.app

import android.os.Bundle
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import com.iptvplayer.exocore.channel.PlayerMethodChannel
import com.iptvplayer.exocore.channel.PlayerEventChannel
import com.iptvplayer.exocore.player.core.ExoPlayerManager
import com.iptvplayer.exocore.player.ui.PlayerViewManager

class MainActivity : FlutterActivity() {

    private var playerManager: ExoPlayerManager? = null
    private var methodChannel: PlayerMethodChannel? = null
    private var eventChannel: PlayerEventChannel? = null

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        val messenger = flutterEngine.dartExecutor.binaryMessenger

        // ── Event channel (Native → Flutter) ──────────────────────────────────
        eventChannel = PlayerEventChannel(messenger)

        // ── ExoPlayer Manager ─────────────────────────────────────────────────
        playerManager = ExoPlayerManager(
            context = applicationContext,
            onStateChange = { state ->
                runOnUiThread {
                    eventChannel?.sendEvent(state.toMap())
                }
            },
            onDebugInfo = { info ->
                runOnUiThread {
                    eventChannel?.sendEvent(info)
                }
            },
            onError = { msg ->
                runOnUiThread {
                    eventChannel?.sendEvent(mapOf(
                        "type"    to "error",
                        "message" to msg,
                    ))
                }
            },
        )

        // ── Method channel (Flutter → Native) ─────────────────────────────────
        methodChannel = PlayerMethodChannel(
            messenger = messenger,
            playerManagerProvider = { playerManager },
        )

        // ── Register native PlayerView for Flutter AndroidView ─────────────────
        flutterEngine.platformViewsController.registry.registerViewFactory(
            "com.iptvplayer/player_view",
            PlayerViewManager { playerManager },
        )
    }

    override fun onDestroy() {
        playerManager?.dispose()
        methodChannel?.dispose()
        eventChannel?.dispose()
        playerManager = null
        super.onDestroy()
    }
}
