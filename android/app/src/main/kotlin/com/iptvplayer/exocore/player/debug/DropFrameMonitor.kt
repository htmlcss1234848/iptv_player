package com.iptvplayer.exocore.player.debug

/**
 * Tracks dropped frames over a sliding window for display purposes.
 * A high drop rate in a short window signals performance issues.
 */
class DropFrameMonitor(private val windowMs: Long = 5000L) {
    data class DropEvent(val count: Int, val timestampMs: Long)

    private val events = ArrayDeque<DropEvent>()

    fun record(count: Int) {
        val now = System.currentTimeMillis()
        events.addLast(DropEvent(count, now))
        // Remove events outside window
        while (events.isNotEmpty() && now - events.first().timestampMs > windowMs) {
            events.removeFirst()
        }
    }

    /** Dropped frames in the last [windowMs] ms */
    val recentDrops: Int get() = events.sumOf { it.count }

    /** Total dropped frames since reset */
    var totalDrops: Int = 0
        private set

    fun recordTotal(count: Int) {
        totalDrops += count
        record(count)
    }

    fun reset() {
        events.clear()
        totalDrops = 0
    }
}
