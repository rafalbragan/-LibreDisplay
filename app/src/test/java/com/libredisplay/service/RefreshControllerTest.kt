package com.libredisplay.service

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RefreshControllerTest {

    @Test
    fun ticks_emitsImmediatelyOnStart() = runTest {
        val controller = RefreshController(intervalMs = 1_000L)
        val firstTick = async { controller.ticks().first() }

        runCurrent()

        assertTrue(firstTick.isCompleted)
    }

    @Test
    fun ticks_emitsAtConfiguredIntervalAfterInitialTick() = runTest {
        val controller = RefreshController(intervalMs = 1_000L)
        val emissionTimes = mutableListOf<Long>()

        val job = launch {
            controller.ticks().take(3).collect {
                emissionTimes += testScheduler.currentTime
            }
        }

        runCurrent()
        advanceTimeBy(2_000)
        runCurrent()
        job.join()

        assertEquals(listOf(0L, 1_000L, 2_000L), emissionTimes)
    }

    @Test
    fun stop_preventsFurtherTicks_untilResume() = runTest {
        val controller = RefreshController(intervalMs = 1_000L)
        val emissionTimes = mutableListOf<Long>()

        val job = launch {
            controller.ticks().take(3).collect {
                emissionTimes += testScheduler.currentTime
                if (emissionTimes.size == 1) {
                    controller.stop()
                }
            }
        }

        runCurrent()
        advanceTimeBy(3_000)
        runCurrent()
        assertEquals(listOf(0L), emissionTimes)
        assertFalse(job.isCompleted)

        controller.resume()
        runCurrent()
        advanceTimeBy(2_000)
        runCurrent()
        job.join()

        assertEquals(listOf(0L, 3_000L, 4_000L), emissionTimes)
    }
}
