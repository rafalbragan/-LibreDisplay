package com.libredisplay.ui.monitoring

import kotlin.math.pow

class PollingBackoffPolicy {
    fun nextDelaySeconds(failureCount: Int, retryAfterSeconds: Long? = null): Long {
        if (retryAfterSeconds != null && retryAfterSeconds > 0) {
            return retryAfterSeconds.coerceAtMost(10 * 60L)
        }
        val clamped = failureCount.coerceIn(1, 8)
        val base = 15.0 * 2.0.pow((clamped - 1).toDouble())
        return base.toLong().coerceIn(15L, 10 * 60L)
    }
}

