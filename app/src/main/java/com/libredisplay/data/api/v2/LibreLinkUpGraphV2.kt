package com.libredisplay.data.api.v2

import com.google.gson.JsonObject

/**
 * Graph payload from /llu/connections/{patientId}/graph for the V2 flow.
 */
data class LibreLinkUpGraphV2(
    val patientId: String,
    val payload: JsonObject
)

