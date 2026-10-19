package com.libredisplay.ui.monitoring

import kotlin.math.roundToLong

internal data class ExplorerWindow(val start: Long, val end: Long) {
    init { require(end > start) }
    val duration: Long get() = end - start
    fun fraction(time: Long): Float = ((time - start).toDouble() / duration.toDouble()).toFloat()
    fun at(fraction: Float): Long = start + (duration.toDouble() * fraction.coerceIn(0f, 1f)).roundToLong()
    fun constrained(bounds: ExplorerWindow): ExplorerWindow {
        val length = duration.coerceIn(minOf(15 * 60_000L, bounds.duration), bounds.duration)
        val left = start.coerceIn(bounds.start, bounds.end - length)
        return ExplorerWindow(left, left + length)
    }
    fun transform(zoom: Float, panFraction: Float, anchorFraction: Float, bounds: ExplorerWindow): ExplorerWindow {
        if (!zoom.isFinite() || zoom <= 0 || !panFraction.isFinite() || !anchorFraction.isFinite()) return this
        val length = (duration.toDouble() / zoom.toDouble()).roundToLong()
            .coerceIn(minOf(15 * 60_000L, bounds.duration), bounds.duration)
        val anchor = anchorFraction.coerceIn(0f, 1f).toDouble()
        val left = start + ((duration - length) * anchor - panFraction.toDouble() * duration).roundToLong()
        return ExplorerWindow(left, left + length).constrained(bounds)
    }
}

/** Aggregated bins are ranges, not invented intermediate samples; gaps never get interpolated. */
internal fun canJoinHistorySamples(previousTime: Long, nextTime: Long): Boolean =
    nextTime > previousTime && nextTime - previousTime <= 20 * 60_000L

