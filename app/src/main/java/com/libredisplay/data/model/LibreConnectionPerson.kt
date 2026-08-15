package com.libredisplay.data.model

import com.libredisplay.data.model.GlucoseReading

data class LibreConnectionPerson(
    val patientId: String,
    val displayName: String,
    val firstName: String? = null,
    val lastName: String? = null
) {
    companion object {
        fun fromNames(
            patientId: String,
            firstName: String?,
            lastName: String?,
            index: Int
        ): LibreConnectionPerson {
            val normalizedFirstName = firstName?.trim().takeIf { !it.isNullOrBlank() }
            val normalizedLastName = lastName?.trim().takeIf { !it.isNullOrBlank() }
            val displayName = when {
                normalizedFirstName != null && normalizedLastName != null -> "$normalizedFirstName $normalizedLastName"
                normalizedFirstName != null -> normalizedFirstName
                normalizedLastName != null -> normalizedLastName
                else -> "Osoba ${index + 1}"
            }
            return LibreConnectionPerson(
                patientId = patientId,
                displayName = displayName,
                firstName = normalizedFirstName,
                lastName = normalizedLastName
            )
        }
    }
}

data class MonitoringSnapshot(
    val persons: List<LibreConnectionPerson>,
    val selectedPerson: LibreConnectionPerson,
    val reading: GlucoseReading
)


