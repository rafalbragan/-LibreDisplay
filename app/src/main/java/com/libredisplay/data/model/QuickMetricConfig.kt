package com.libredisplay.data.model

enum class QuickMetricId(val storageId: String) {
    BELOW("below"),
    IN_RANGE("in_range"),
    ABOVE("above"),
    GMI("gmi"),
    HBA1C("hba1c"),
    AVERAGE("average"),
    SENSOR_ACTIVITY("sensor_activity");

    companion object {
        val DEFAULT_ORDER: List<QuickMetricId> = listOf(
            BELOW,
            IN_RANGE,
            ABOVE,
            GMI,
            HBA1C,
            AVERAGE,
            SENSOR_ACTIVITY
        )

        val DEFAULT_VISIBLE: Set<QuickMetricId> = setOf(
            BELOW,
            IN_RANGE,
            ABOVE,
            GMI,
            HBA1C,
            AVERAGE,
            SENSOR_ACTIVITY
        )

        fun fromStorageId(value: String): QuickMetricId? = entries.firstOrNull { it.storageId == value }

        fun normalizeOrder(ids: List<QuickMetricId>): List<QuickMetricId> {
            val unique = ids.distinct()
            val missing = DEFAULT_ORDER.filterNot { it in unique }
            return unique + missing
        }
    }
}

