package com.libredisplay.data.demo

object DemoDataPolicy {
    const val DEMO_PATIENT_PREFIX = "demo-person-"
    const val SOURCE_DEMO = "DEMO"
    const val LEGACY_SOURCE_DEMO_MODE = "DemoMode"

    fun isDemoPatientId(patientId: String?): Boolean {
        return patientId?.startsWith(DEMO_PATIENT_PREFIX) == true
    }

    fun isDemoSource(source: String?): Boolean {
        if (source.isNullOrBlank()) return false
        return source.equals(SOURCE_DEMO, ignoreCase = true) ||
            source.equals(LEGACY_SOURCE_DEMO_MODE, ignoreCase = true)
    }
}

