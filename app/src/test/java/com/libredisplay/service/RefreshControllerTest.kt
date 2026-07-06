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
    fun ticks_delaysFirstEmissionUntilConfiguredInterval() = runTest {
        val controller = RefreshController(intervalMs = 1_000L)
        val firstTick = async { controller.ticks().first() }

        runCurrent()
        assertFalse(firstTick.isCompleted)

        advanceTimeBy(999)
        runCurrent()
        assertFalse(firstTick.isCompleted)

        advanceTimeBy(1)
        runCurrent()

        assertTrue(firstTick.isCompleted)
    }

    @Test
    fun ticks_emitsAtConfiguredInterval() = runTest {
        val controller = RefreshController(intervalMs = 1_000L)
        val emissionTimes = mutableListOf<Long>()

        val job = launch {
            controller.ticks().take(3).collect {
                emissionTimes += testScheduler.currentTime
            }
        }

        runCurrent()
        advanceTimeBy(3_000)
        runCurrent()
        job.join()

        assertEquals(listOf(1_000L, 2_000L, 3_000L), emissionTimes)
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
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(listOf(1_000L), emissionTimes)

        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(listOf(1_000L), emissionTimes)

        controller.resume()
        runCurrent()
        advanceTimeBy(2_000)
        runCurrent()
        job.join()

        assertEquals(listOf(1_000L, 4_000L, 5_000L), emissionTimes)
    }
}
