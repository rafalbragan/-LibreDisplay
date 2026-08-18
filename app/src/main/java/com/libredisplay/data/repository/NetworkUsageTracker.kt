package com.libredisplay.data.repository

import com.libredisplay.data.model.AppMode
import com.libredisplay.data.storage.SecureStorage

class NetworkUsageTracker(
    private val storage: SecureStorage,
    private val appModeProvider: () -> AppMode
) {

    fun recordRequest(uploadedBytes: Long) {
        if (appModeProvider() == AppMode.DEMO) return
        val now = System.currentTimeMillis()
        val currentUploaded = readLong(SecureStorage.KEY_NETWORK_TOTAL_UPLOADED_BYTES)
        val currentRequests = readLong(SecureStorage.KEY_NETWORK_REQUEST_COUNT)

        writeLong(SecureStorage.KEY_NETWORK_TOTAL_UPLOADED_BYTES, currentUploaded + uploadedBytes.coerceAtLeast(0))
        writeLong(SecureStorage.KEY_NETWORK_REQUEST_COUNT, currentRequests + 1)
        touchMeasuredWindow(now)
    }

    fun recordResponse(downloadedBytes: Long, success: Boolean) {
        if (appModeProvider() == AppMode.DEMO) return
        val now = System.currentTimeMillis()
        val currentDownloaded = readLong(SecureStorage.KEY_NETWORK_TOTAL_DOWNLOADED_BYTES)
        writeLong(SecureStorage.KEY_NETWORK_TOTAL_DOWNLOADED_BYTES, currentDownloaded + downloadedBytes.coerceAtLeast(0))

        val successKey = SecureStorage.KEY_NETWORK_SUCCESS_COUNT
        val failedKey = SecureStorage.KEY_NETWORK_FAILED_COUNT
        if (success) {
            writeLong(successKey, readLong(successKey) + 1)
            writeLong(SecureStorage.KEY_NETWORK_LAST_SYNC_SUCCESS, 1)
            writeLong(SecureStorage.KEY_NETWORK_LAST_SYNC_AT, now)
        } else {
            writeLong(failedKey, readLong(failedKey) + 1)
            writeLong(SecureStorage.KEY_NETWORK_LAST_SYNC_SUCCESS, 0)
            writeLong(SecureStorage.KEY_NETWORK_LAST_SYNC_AT, now)
        }
        touchMeasuredWindow(now)
    }

    private fun touchMeasuredWindow(nowMillis: Long) {
        if (readLong(SecureStorage.KEY_NETWORK_FIRST_MEASURED_AT) <= 0L) {
            writeLong(SecureStorage.KEY_NETWORK_FIRST_MEASURED_AT, nowMillis)
        }
        writeLong(SecureStorage.KEY_NETWORK_LAST_MEASURED_AT, nowMillis)
    }

    private fun readLong(key: String): Long = storage.getString(key).toLongOrNull() ?: 0L

    private fun writeLong(key: String, value: Long) {
        storage.putString(key, value.coerceAtLeast(0).toString())
    }
}

