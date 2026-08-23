package com.libredisplay.data.model

enum class QuickMetricId(val storageId: String) {
    BELOW("below"),
    IN_RANGE("in_range"),
    ABOVE("above"),
    AVERAGE("average"),
    MINIMUM("minimum"),
    MAXIMUM("maximum"),
    GMI("gmi"),
    VERY_LOW_EPISODES("very_low_episodes"),
    VERY_HIGH_EPISODES("very_high_episodes"),
    HBA1C("hba1c"),
    SENSOR_ACTIVITY("sensor_activity");

    companion object {
        val DEFAULT_ORDER: List<QuickMetricId> = listOf(
            BELOW,
            IN_RANGE,
            ABOVE,
            AVERAGE,
            MINIMUM,
            MAXIMUM,
            GMI,
            VERY_LOW_EPISODES,
            VERY_HIGH_EPISODES
        )

        val DEFAULT_VISIBLE: Set<QuickMetricId> = setOf(
            BELOW,
            IN_RANGE,
            ABOVE,
            AVERAGE,
            MINIMUM,
            MAXIMUM,
            GMI,
            VERY_LOW_EPISODES,
            VERY_HIGH_EPISODES
        )

        fun fromStorageId(value: String): QuickMetricId? = entries.firstOrNull { it.storageId == value }

        fun normalizeOrder(ids: List<QuickMetricId>): List<QuickMetricId> {
            val unique = ids.distinct()
            val missing = DEFAULT_ORDER.filterNot { it in unique }
            return unique + missing
        }
    }
}

