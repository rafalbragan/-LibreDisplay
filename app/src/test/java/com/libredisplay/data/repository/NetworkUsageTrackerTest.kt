package com.libredisplay.data.repository

import android.app.Application
import com.libredisplay.data.model.AppMode
import com.libredisplay.data.storage.SecureStorage
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class NetworkUsageTrackerTest {

    @Test
    fun tracker_recordsUploadDownload_forLiveMode() {
        val context = RuntimeEnvironment.getApplication()
        val storage = SecureStorage(context)
        storage.clear()

        val tracker = NetworkUsageTracker(storage) { AppMode.LIVE }
        tracker.recordRequest(123)
        tracker.recordResponse(downloadedBytes = 456, success = true)

        assertEquals("123", storage.getString(SecureStorage.KEY_NETWORK_TOTAL_UPLOADED_BYTES))
        assertEquals("456", storage.getString(SecureStorage.KEY_NETWORK_TOTAL_DOWNLOADED_BYTES))
        assertEquals("1", storage.getString(SecureStorage.KEY_NETWORK_REQUEST_COUNT))
        assertEquals("1", storage.getString(SecureStorage.KEY_NETWORK_SUCCESS_COUNT))
    }

    @Test
    fun tracker_ignoresDemoMode() {
        val context = RuntimeEnvironment.getApplication()
        val storage = SecureStorage(context)
        storage.clear()

        val tracker = NetworkUsageTracker(storage) { AppMode.DEMO }
        tracker.recordRequest(123)
        tracker.recordResponse(downloadedBytes = 456, success = true)

        assertEquals("", storage.getString(SecureStorage.KEY_NETWORK_TOTAL_UPLOADED_BYTES))
        assertEquals("", storage.getString(SecureStorage.KEY_NETWORK_TOTAL_DOWNLOADED_BYTES))
    }
}

