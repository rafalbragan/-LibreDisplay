package com.libredisplay.data.model

/**
 * Direction of glucose change, derived from the LibreLinkUp trend integer.
 *
 * LibreLinkUp API trend codes verified against public LibreView docs and
 * DiaKEM/libre-link-up-api-client:
 *   1 = SingleDown     (↓)
 *   2 = FortyFiveDown  (↘)
 *   3 = Flat           (→)
 *   4 = FortyFiveUp    (↗)
 *   5 = SingleUp       (↑)
 */
enum class GlucoseTrend(val arrow: String, val description: String) {
    RISING_FAST("↑", "Szybko rośnie"),
    RISING("↗", "Rośnie"),
    FLAT("→", "Stabilnie"),
    FALLING("↘", "Spada"),
    FALLING_FAST("↓", "Szybko spada"),
    UNKNOWN("?", "Nieznany");

    companion object {
        /** Map LibreLinkUp numeric trend code to enum value. */
        fun fromApiCode(code: Int): GlucoseTrend = when (code) {
            1 -> FALLING_FAST
            2 -> FALLING
            3 -> FLAT
            4 -> RISING
            5 -> RISING_FAST
            else -> UNKNOWN
        }

        fun fromSlope(mgDlPerMinute: Double): GlucoseTrend = when {
            mgDlPerMinute >= 3.0 -> RISING_FAST
            mgDlPerMinute >= 1.0 -> RISING
            mgDlPerMinute <= -3.0 -> FALLING_FAST
            mgDlPerMinute <= -1.0 -> FALLING
            kotlin.math.abs(mgDlPerMinute) < 1.0 -> FLAT
            else -> UNKNOWN
        }
    }
}
