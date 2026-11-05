package com.libredisplay.data.demo

/**
 * Controlled demo scenarios for the DEBUG build scenario selector.
 *
 * In release builds the scenario selector is not shown and [DemoScenarioController.selectScenario]
 * is a no-op, so production/release Live behaviour is unchanged.
 */
enum class DemoScenario(val displayName: String) {
    MIKA_DEMO("Mika DEMO"),
    NORMAL("Normalny"),
    RAPID_RISE("Szybki wzrost"),
    RAPID_FALL("Szybki spadek"),
    HYPO("Hipoglikemia"),
    SEVERE_HYPO("Ciężka hipoglikemia"),
    HYPER("Hiperglikemia"),
    STALE_DATA("Stare dane"),
    MISSING_DATA("Brak danych"),
    MULTIPLE_PATIENTS_ONE_AT_RISK("Kilka osób – jedna w ryzyku")
}
