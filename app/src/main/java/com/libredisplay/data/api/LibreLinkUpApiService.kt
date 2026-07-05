package com.libredisplay.data.api

import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

data class LoginRequest(
    val email: String,
    val password: String
)

interface LibreLinkUpApiService {
    @Headers("Content-Type: application/json")
    @POST("/llu/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<JsonObject>

    @GET("/llu/connections")
    suspend fun getConnections(
        @Header("Authorization") authorization: String
    ): Response<JsonObject>

    @GET("/llu/connections/{connectionId}/graph")
    suspend fun getLatestGraph(
        @Header("Authorization") authorization: String,
        @Path("connectionId") connectionId: String
    ): Response<JsonObject>
}

