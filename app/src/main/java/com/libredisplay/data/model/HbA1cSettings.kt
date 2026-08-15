package com.libredisplay.data.model

import java.time.LocalDate

data class HbA1cSettings(
    val patientId: String?,
    val labHbA1cPercent: Double? = null,
    val labHbA1cDate: LocalDate? = null,
    val targetHbA1cPercent: Double = 7.5
)

