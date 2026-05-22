package com.iptvplayer.exocore.channel

import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel

/**
 * Sends events from Native → Flutter.
 * Events: state changes, debug info, errors.
 */
class PlayerEventChannel(messenger: BinaryMessenger) : EventChannel.StreamHandler {

    private val eventChannel = EventChannel(messenger, "com.iptvplayer/exoplayer_events")
    private var eventSink: EventChannel.EventSink? = null

    init {
        eventChannel.setStreamHandler(this)
    }

    override fun onListen(arguments: Any?, events: EventChannel.EventSink) {
        eventSink = events
    }

    override fun onCancel(arguments: Any?) {
        eventSink = null
    }

    fun sendEvent(data: Map<String, Any?>) {
        eventSink?.success(data)
    }

    fun sendError(code: String, message: String) {
        eventSink?.error(code, message, null)
    }

    fun dispose() {
        eventSink?.endOfStream()
        eventSink = null
        eventChannel.setStreamHandler(null)
    }
}
