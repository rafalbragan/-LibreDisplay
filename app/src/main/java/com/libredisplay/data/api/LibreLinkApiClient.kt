package com.libredisplay.data.api

import android.util.Log
import com.google.gson.JsonObject
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

private const val TAG = "LibreLinkApiClient"

open class LibreLinkApiClient(
    private val productHeader: String = "llu.android",
    private val versionHeader: String = "4.17.0"
) {

    open suspend fun login(region: String, email: String, password: String): LoginResult {
        val candidates = regionCandidates(region)
        var lastError: LibreLinkUpHttpException? = null

        for (candidate in candidates) {
            val service = createService(candidate.baseUrl)
            val response = service.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val body = response.body() ?: throw LibreLinkUpException("Brak odpowiedzi z serwera LibreLinkUp")
                val token = firstString(body, listOf("token", "authTicket", "jwtToken", "accessToken"))
                    ?: throw LibreLinkUpException("Nie znaleziono tokenu sesji w odpowiedzi")

                Log.i(TAG, "Login OK region=${candidate.code} endpoint=${candidate.baseUrl}")
                return LoginResult(token = token, region = candidate.code)
            }

            val errorBody = runCatching { response.errorBody()?.string().orEmpty() }.getOrDefault("")
            val ex = LibreLinkUpHttpException(response.code(), errorBody)
            lastError = ex
            Log.w(
                TAG,
                "Login failed endpoint=${candidate.baseUrl} region=${candidate.code} status=${response.code()} hasProductVersion=true"
            )

            if (response.code() == 430 || response.code() == 429) break
        }

        throw lastError ?: LibreLinkUpException("Logowanie do LibreLinkUp nie powiodlo sie")
    }

    open suspend fun getConnections(region: String, token: String): Response<JsonObject> {
        return createService(baseUrlForRegion(region)).getConnections("Bearer $token")
    }

    open suspend fun getLatestGraph(region: String, token: String, connectionId: String): Response<JsonObject> {
        return createService(baseUrlForRegion(region)).getLatestGraph("Bearer $token", connectionId)
    }

    private fun createService(baseUrl: String): LibreLinkUpApiService {
        val headerInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("product", productHeader)
                .header("version", versionHeader)
                .header("cache-control", "no-cache")
                .header("connection", "Keep-Alive")
                .header("User-Agent", "LibreDisplay/1.0 (Android)")
                .build()
            chain.proceed(request)
        }

        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .addInterceptor(logging)
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(LibreLinkUpApiService::class.java)
    }

    private fun baseUrlForRegion(regionCode: String): String = when (regionCode.uppercase()) {
        "EU2" -> "https://api-eu2.libreview.io/"
        "US" -> "https://api-us.libreview.io/"
        "DE" -> "https://api-de.libreview.io/"
        "FR" -> "https://api-fr.libreview.io/"
        else -> "https://api-eu.libreview.io/"
    }

    private fun regionCandidates(regionCode: String): List<RegionCandidate> {
        return when (regionCode.uppercase()) {
            "EU" -> listOf(
                RegionCandidate("EU", "https://api-eu.libreview.io/"),
                RegionCandidate("EU2", "https://api-eu2.libreview.io/")
            )
            else -> listOf(RegionCandidate(regionCode.uppercase(), baseUrlForRegion(regionCode)))
        }
    }

    private fun firstString(body: JsonObject, keys: List<String>): String? {
        keys.forEach { key ->
            val node = body.get(key)
            if (node != null && node.isJsonPrimitive && node.asJsonPrimitive.isString) {
                return node.asString
            }
        }
        body.entrySet().forEach { (_, value) ->
            if (value.isJsonObject) {
                keys.forEach { nestedKey ->
                    val nested = value.asJsonObject.get(nestedKey)
                    if (nested != null && nested.isJsonPrimitive && nested.asJsonPrimitive.isString) {
                        return nested.asString
                    }
                }
            }
        }
        return null
    }
}

data class LoginResult(
    val token: String,
    val region: String
)

data class RegionCandidate(
    val code: String,
    val baseUrl: String
)

