package com.libredisplay.service

import android.app.Application
import android.content.pm.ServiceInfo
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class MonitoringServiceTimeoutTest {
    @Test fun dataSyncTimeoutStopsServiceWithoutStartingNetworkWork() {
        // Attach only: onCreate must not start a real polling loop in this unit test.
        val service = Robolectric.buildService(MonitoringService::class.java).get()
        service.onTimeout(1, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        assertTrue(shadowOf(service).isStoppedBySelf)
    }

    @Test fun repeatedTimeoutIsSafe() {
        val service = Robolectric.buildService(MonitoringService::class.java).get()
        repeat(2) { service.onTimeout(1, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) }
        assertTrue(shadowOf(service).isStoppedBySelf)
    }
}
