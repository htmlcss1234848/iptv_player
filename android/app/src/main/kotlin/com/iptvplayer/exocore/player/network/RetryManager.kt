package com.iptvplayer.exocore.player.network

import android.os.Handler
import android.os.Looper

class RetryManager(
    private val maxRetries: Int = 3,
    private val retryDelayMs: Long = 3000L,
    private val onRetry: (attempt: Int) -> Unit,
) {
    private var retryCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private var retryRunnable: Runnable? = null

    fun onError() {
        if (retryCount < maxRetries) {
            retryCount++
            scheduleRetry()
        }
    }

    fun reset() {
        retryCount = 0
        cancelPending()
    }

    fun forceRetry() {
        retryCount = 0
        cancelPending()
        onRetry(0)
    }

    private fun scheduleRetry() {
        cancelPending()
        val attempt = retryCount
        retryRunnable = Runnable { onRetry(attempt) }.also { r ->
            handler.postDelayed(r, retryDelayMs * attempt)
        }
    }

    private fun cancelPending() {
        retryRunnable?.let { handler.removeCallbacks(it) }
        retryRunnable = null
    }

    fun destroy() {
        cancelPending()
    }

    val hasRetriesLeft get() = retryCount < maxRetries
    val currentAttempt get() = retryCount
}
