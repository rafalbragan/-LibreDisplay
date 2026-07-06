package com.libredisplay.data.api

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LibreGraphResponseModelTest {

    private val gson = Gson()

    @Test
    fun graphData_isReadFromDataGraphData() {
        val response = gson.fromJson(
            """
            {
              "status": 0,
              "data": {
                "connection": {
                  "glucoseMeasurement": { "ValueInMgPerDl": 120, "Timestamp": "2026-07-06T10:00:00Z" }
                },
                "graphData": [
                  { "ValueInMgPerDl": 105, "Timestamp": "2026-07-06T09:45:00Z" },
                  { "ValueInMgPerDl": 168, "Timestamp": "2026-07-06T10:00:00Z" }
                ]
              }
            }
            """.trimIndent(),
            LibreGraphResponse::class.java
        )

        assertEquals(2, response.data?.graphData?.size)
        assertEquals(105.0, response.data?.graphData?.first()?.valueInMgPerDl)
    }

    @Test
    fun graphData_isNotExpectedInsideConnection() {
        val response = gson.fromJson(
            """
            {
              "status": 0,
              "data": {
                "connection": {
                  "glucoseMeasurement": { "ValueInMgPerDl": 120, "Timestamp": "2026-07-06T10:00:00Z" },
                  "graphData": [
                    { "ValueInMgPerDl": 105, "Timestamp": "2026-07-06T09:45:00Z" }
                  ]
                }
              }
            }
            """.trimIndent(),
            LibreGraphResponse::class.java
        )

        assertNull(response.data?.graphData)
    }

    @Test
    fun currentMeasurement_isReadFromDataConnectionGlucoseMeasurement() {
        val response = gson.fromJson(
            """
            {
              "status": 0,
              "data": {
                "connection": {
                  "glucoseMeasurement": {
                    "ValueInMgPerDl": 142,
                    "Value": 142,
                    "TrendArrow": 4,
                    "Timestamp": "2026-07-06T10:00:00Z"
                  }
                }
              }
            }
            """.trimIndent(),
            LibreGraphResponse::class.java
        )

        val measurement = response.data?.connection?.glucoseMeasurement
        assertNotNull(measurement)
        assertEquals(142.0, measurement?.valueInMgPerDl)
        assertEquals(4, measurement?.trendArrow)
    }
}

