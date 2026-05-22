package com.iptvplayer.exocore.player.ui

import android.content.Context
import android.view.View
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory
import com.iptvplayer.exocore.player.core.ExoPlayerManager

/**
 * Provides the native ExoPlayer PlayerView to Flutter via PlatformView.
 * This is registered in MainActivity and embedded via AndroidView in Flutter.
 */
class PlayerViewManager(
    private val playerManagerProvider: () -> ExoPlayerManager?
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {

    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        return ExoPlayerPlatformView(context, playerManagerProvider)
    }
}

class ExoPlayerPlatformView(
    context: Context,
    private val playerManagerProvider: () -> ExoPlayerManager?,
) : PlatformView {

    private val playerView = PlayerView(context).apply {
        useController = false // We use Flutter's own controls overlay
        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
        keepScreenOn = true
    }

    init {
        // Attach the current player if one exists
        playerManagerProvider()?.attachView(playerView)
    }

    override fun getView(): View = playerView

    override fun dispose() {
        playerManagerProvider()?.detachView(playerView)
    }
}
