package com.libredisplay.data.model

/**
 * Direction of glucose change, derived from the LibreLinkUp trend integer.
 *
 * LibreLinkUp API trend codes (for reference when implementing RealLibreLinkUpClient):
 *   1 = Rising fast   (↑↑)
 *   2 = Rising        (↑)
 *   3 = Rising slowly (↗)
 *   4 = Flat          (→)
 *   5 = Falling slowly(↘)
 *   6 = Falling       (↓)
 *   7 = Falling fast  (↓↓)
 */
enum class GlucoseTrend(val arrow: String, val description: String) {
    RISING_FAST("↑↑", "Rising Fast"),
    RISING("↑", "Rising"),
    FLAT("→", "Stable"),
    FALLING("↓", "Falling"),
    FALLING_FAST("↓↓", "Falling Fast"),
    UNKNOWN("?", "Unknown");

    companion object {
        /** Map LibreLinkUp numeric trend code to enum value. */
        fun fromApiCode(code: Int): GlucoseTrend = when (code) {
            1 -> RISING_FAST
            2 -> RISING
            3 -> RISING
            4 -> FLAT
            5 -> FALLING
            6 -> FALLING
            7 -> FALLING_FAST
            else -> UNKNOWN
        }
    }
}

