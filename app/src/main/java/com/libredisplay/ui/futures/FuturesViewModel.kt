package com.libredisplay.ui.futures

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class FuturesAudience(val label: String) {
    WSZYSCY("Wszyscy"),
    PACJENT("Pacjent"),
    SENIOR("Senior"),
    OPIEKUN("Opiekun"),
    LEKARZ("Lekarz")
}

enum class FuturesStatus(val label: String) {
    AVAILABLE("Dostępne"),
    IN_DEVELOPMENT("W rozwoju"),
    PLANNED("Planowane"),
    UNDER_REVIEW("Weryfikacja")
}

data class FutureIdea(
    val id: String,
    val emoji: String,
    val title: String,
    val summary: String,
    val status: FuturesStatus,
    val audiences: Set<FuturesAudience>,
    val nowValue: String,
    val highlights: List<String>,
    val nextStep: String
)

data class FuturesUiState(
    val selectedAudience: FuturesAudience = FuturesAudience.WSZYSCY,
    val expandedIdeaIds: Set<String> = setOf("history-and-chart"),
    val ideas: List<FutureIdea> = defaultFutureIdeas()
) {
    val visibleIdeas: List<FutureIdea>
        get() = ideas.filter { idea ->
            selectedAudience == FuturesAudience.WSZYSCY || selectedAudience in idea.audiences
        }
}

class FuturesViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(FuturesUiState())
    val uiState: StateFlow<FuturesUiState> = _uiState.asStateFlow()

    fun selectAudience(audience: FuturesAudience) {
        _uiState.update { it.copy(selectedAudience = audience) }
    }

    fun toggleIdea(id: String) {
        _uiState.update { state ->
            val expanded = state.expandedIdeaIds.toMutableSet()
            if (!expanded.add(id)) expanded.remove(id)
            state.copy(expandedIdeaIds = expanded)
        }
    }
}

private fun defaultFutureIdeas(): List<FutureIdea> = listOf(
    FutureIdea(
        id = "monitoring-glucose",
        emoji = "📈",
        title = "Monitoring glikemii",
        summary = "Aktualna wartość glikemii, trend, status świeżości danych i szybkie podsumowanie ryzyka na ekranie głównym.",
        status = FuturesStatus.AVAILABLE,
        audiences = FuturesAudience.entries.toSet(),
        nowValue = "Dostępne na bazowym ekranie monitorowania w LibreCare 2.17.2: aktualny poziom glikemii, trend i status danych są widoczne bez dodatkowego przejścia.",
        highlights = listOf(
            "Wskaźnik aktualności danych i kolorowe ostrzeżenia dla użytkownika.",
            "Szybki przegląd wartości, trendu i zakresu docelowego.",
            "Wspólna podstawa dla ekranu głównego, opiekuna i analityki."
        ),
        nextStep = "To jest obecnie podstawowa funkcja produkcyjna i nie jest traktowana jako przyszły prototyp."
    ),
    FutureIdea(
        id = "history-and-chart",
        emoji = "📊",
        title = "Historia i interaktywny wykres",
        summary = "Pełna historia glikemii z interaktywnym wykresem, zakresem czasu i kontrolą zoomu w widoku głównym i pełnoekranowym.",
        status = FuturesStatus.AVAILABLE,
        audiences = FuturesAudience.entries.toSet(),
        nowValue = "Wersja bazowa ma działający wykres historii, zoom i wczytanie zakresu czasu bez rozrywania głównego workflowu.",
        highlights = listOf(
            "Po naciśnięciu wykres otwiera się w pełnym widoku historii.",
            "Zakresy czasu i min/max są zachowane między ekranami.",
            "Teren z zakresem docelowym i siatka są czytelne w warunkach produkcyjnych."
        ),
        nextStep = "To już jest funkcjonalność w bazowym obrazie produktu, nie kandydat do odkrycia."
    ),
    FutureIdea(
        id = "libreview-csv-import",
        emoji = "📥",
        title = "Import LibreView CSV",
        summary = "Import danych z eksportu LibreView CSV do bieżącego modelu raportowania i analizy bez zmiany zasad bezpieczeństwa.",
        status = FuturesStatus.AVAILABLE,
        audiences = FuturesAudience.entries.toSet(),
        nowValue = "Import jest dostarczony w bazie PR #46 i jest częścią obecnej linii produkcyjnej LibreCare 2.17.2.",
        highlights = listOf(
            "Wsparcie dla eksportu LibreView CSV jako źródła danych w aplikacji.",
            "Dane ładowane do istniejącego modelu analitycznego bez łamania aktualnego UI.",
            "Wersja 2.17.2 jest zgodna z dostarczoną funkcjonalnością PR #46."
        ),
        nextStep = "Dla następnej iteracji: utrzymać jako sprawdzoną ścieżkę importu, a nie jako przyszły prototyp."
    ),
    FutureIdea(
        id = "analytics-and-statistics",
        emoji = "🧮",
        title = "Analizy i statystyki",
        summary = "Miary, TIR, średnie, rozkłady i statystyki dla wybranego zakresu czasu z czytelnym podsumowaniem użytkownika.",
        status = FuturesStatus.AVAILABLE,
        audiences = FuturesAudience.entries.toSet(),
        nowValue = "Analiza i statystyki są udostępnione w istniejącym modulo analitycznym, z zakresem, metrykami i prezentacją wyników.",
        highlights = listOf(
            "Wizualizacja zakresu czasu i trendu w analizie.",
            "Zestaw metryk i istotnych wartości dla użytkownika i opiekuna.",
            "Statystyki pozostają spójne z monitoringiem i historią."
        ),
        nextStep = "To jest obecna funkcja produktu, nie nowy pomysł z dyskusji."
    ),
    FutureIdea(
        id = "person-profiles",
        emoji = "👥",
        title = "Profile obserwowanych osób",
        summary = "Osoby monitorowane są rozdzielone i selekcjonowane, z priorytetyzacją i identyfikacją na kartach oraz w widoku opiekuna.",
        status = FuturesStatus.AVAILABLE,
        audiences = setOf(FuturesAudience.OPIEKUN, FuturesAudience.PACJENT, FuturesAudience.LEKARZ),
        nowValue = "W aktualnym produkcie istnieje wybór osób, osobna logika podziału na sekcje oraz osobne karty monitorowanych profili.",
        highlights = listOf(
            "Wybór osoby bez crash-loop i z rollbackiem po błędzie.",
            "Wysokiej wagi sekcja \"Wymaga uwagi\" dla opiekuna.",
            "Przejrzysta hierarchia profili i statusu danych."
        ),
        nextStep = "Ta funkcja jest już utrwalona w bazowym UI i powinna być prezentowana jako dostępna."
    ),
    FutureIdea(
        id = "privacy-and-data",
        emoji = "🔒",
        title = "Prywatność i dane",
        summary = "Kontrola prywatności, przechowywanie danych, ustawienia użytkownika i widoczne informacje o bezpieczeństwie w oparciu o istniejące ekrany ustawień.",
        status = FuturesStatus.AVAILABLE,
        audiences = FuturesAudience.entries.toSet(),
        nowValue = "Ekrany prywatności, ustawień i diagnostyki są obecne; nie wszystkie warstwy bezpieczeństwa są jeszcze finalnie rozstrzygnięte, ale UI istnieje.",
        highlights = listOf(
            "Prywatność i dane są rozdzielone w ustawieniach aplikacji.",
            "Przechowywanie i retencja są dostępne w istniejącym produkcie.",
            "Użytkownik ma kontrolę nad ustawieniami danych i diagnostyki."
        ),
        nextStep = "To jest funkcja obecna, lecz weryfikacja prywatności i backupu pozostaje ciągłym procesem."
    ),
    FutureIdea(
        id = "backup-and-restore",
        emoji = "💾",
        title = "Kopie i odzyskiwanie danych",
        summary = "Automatyczne kopie zapasowe, import/restore i mechanika zabezpieczająca dane, z zachowaniem ostrożności wobec pozostałych luk integralności.",
        status = FuturesStatus.IN_DEVELOPMENT,
        audiences = setOf(FuturesAudience.OPIEKUN, FuturesAudience.PACJENT, FuturesAudience.LEKARZ),
        nowValue = "W aplikacji jest mechanizm backupu i restore, ale integracja i integralność danych wymaga dalszej weryfikacji; nie należy oznaczać całkowitego bezpieczeństwa.",
        highlights = listOf(
            "Automatyczne kopie zapasowe są częścią istniejącego flow.",
            "Weryfikacja integralności i odtwarzania pozostaje priorytetem.",
            "Niezbędne są dalsze testy przy poprawnie przechowywanych i odzyskiwanych danych."
        ),
        nextStep = "Zaleca się dalszą implementację i testy bezpieczeństwa przed uznaniem za w pełni bezpieczne."
    ),
    FutureIdea(
        id = "caregiver-senior-clinician-modes",
        emoji = "🧭",
        title = "Tryby Opiekun / Senior / Lekarz",
        summary = "Jedna aplikacja z wieloma perspektywami użytkownika, z bieżącym głównym kierunkiem opiekuna i kolejnymi etapami dla innych ról.",
        status = FuturesStatus.IN_DEVELOPMENT,
        audiences = FuturesAudience.entries.toSet(),
        nowValue = "Istnieje model i rozgałęzienie dla opiekuna w Home, ale pełne, trwałe przełączanie i dedykowane ekrany Senior / Lekarz są nadal rozwijane.",
        highlights = listOf(
            "Obecny produkt jest zorientowany na główny tryb opiekuna.",
            "Architektura i perspektywy są już projektowane w warstwie produktu.",
            "Nie ma jeszcze pełnej, produkcyjnej implementacji wszystkich trybów."
        ),
        nextStep = "To jest bezpieczny kandydat do dalszego rozwoju, ale bez uznania za gotowe dla wszystkich ról."
    ),
    FutureIdea(
        id = "alerts-and-refresh",
        emoji = "🔔",
        title = "Alerty i odświeżanie danych",
        summary = "Sygnały ostrzegawcze, regularne odświeżanie danych i spójna prezentacja świeżości w całym interfejsie użytkownika.",
        status = FuturesStatus.UNDER_REVIEW,
        audiences = setOf(FuturesAudience.OPIEKUN, FuturesAudience.PACJENT, FuturesAudience.LEKARZ),
        nowValue = "Jest ślad zachowania i monitorowania, ale obszar ostrzeżeń i odświeżania wymaga dalszej weryfikacji poprzez testy i badania bezpieczeństwa.",
        highlights = listOf(
            "Należy weryfikować zachowanie alertów po zmianach danych i po odświeżeniu.",
            "Problem bezpieczeństwa dotyczy świeżości danych i spójnej komunikacji.",
            "To obszar z wysokim ryzykiem i niskim marginesem błędu dla użytkowników."
        ),
        nextStep = "Wymagane są testy, poprawki i decyzja produktu zanim funkcja będzie mogła być uznana za gotową."
    ),
    FutureIdea(
        id = "tir-and-metrics-accuracy",
        emoji = "🎯",
        title = "Dokładność 12h metryk i TIR",
        summary = "Doprecyzowanie prezentacji TIR, zakresów 12-godzinnych oraz zgodności metryk z realnym dostępem danych i zakresem wyświetlania.",
        status = FuturesStatus.UNDER_REVIEW,
        audiences = setOf(FuturesAudience.LEKARZ, FuturesAudience.OPIEKUN, FuturesAudience.PACJENT),
        nowValue = "To obszar z istotnym ryzykiem jakości oraz z dużym wpływem na zaufanie do aplikacji i interpretację danych klinicznych.",
        highlights = listOf(
            "Należy zweryfikować precyzję i spójność 12h TIR oraz metryk.",
            "Wartość wskaźników musi odpowiadać rzeczywistemu pokryciu danych i okresowi analizy.",
            "To wymaga testów i jasnego produktu wobec danych demo oraz dobowych zmian."
        ),
        nextStep = "Potrzebne jest osobne badanie produktu i kontrola przed zaliczeniem do funkcjonalności gotowej."
    )
)

