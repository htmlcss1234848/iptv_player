package com.iptvplayer.exocore.player.network

import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter

class BandwidthMeterWrapper {
    private var _meter: DefaultBandwidthMeter? = null

    fun attach(meter: DefaultBandwidthMeter) {
        _meter = meter
    }

    val bitrateEstimate: Long
        get() = _meter?.bitrateEstimate ?: 0L

    val formattedSpeed: String
        get() {
            val bps = bitrateEstimate
            return when {
                bps <= 0        -> "-"
                bps >= 1_000_000 -> "${String.format("%.1f", bps / 1_000_000.0)} Mbps"
                bps >= 1_000    -> "${String.format("%.0f", bps / 1_000.0)} Kbps"
                else            -> "$bps bps"
            }
        }
}
