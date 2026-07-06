package com.libredisplay.data.api

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.POST
import retrofit2.http.Path

interface LibreLinkUpApiService {
    @POST("llu/auth/login")
    suspend fun login(
        @HeaderMap headers: Map<String, String>,
        @Body request: LoginRequest
    ): Response<JsonObject>

    @GET("llu/connections")
    suspend fun getConnections(
        @HeaderMap headers: Map<String, String>
    ): Response<JsonObject>

    @GET("llu/connections/{patientId}/graph")
    suspend fun getGraph(
        @HeaderMap headers: Map<String, String>,
        @Path("patientId") patientId: String
    ): Response<JsonObject>
}
