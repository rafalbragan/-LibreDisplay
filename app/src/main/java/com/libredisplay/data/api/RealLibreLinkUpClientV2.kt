package com.libredisplay.data.api

import com.libredisplay.data.model.GlucoseReading

class RealLibreLinkUpClientV2(
    private val region: String,
    private val apiOverride: LibreLinkUpApiService? = null
) : LibreLinkUpClient {

    private val delegate = RetrofitLibreLinkUpClient(initialRegion = region)

    override suspend fun login(email: String, password: String) {
        delegate.login(email, password, region)
    }

    override suspend fun getConnections(): List<String> {
        return delegate.getConnections()
    }

    override suspend fun getLatestReading(): GlucoseReading? {
        return delegate.getLatestReading()
    }
}
