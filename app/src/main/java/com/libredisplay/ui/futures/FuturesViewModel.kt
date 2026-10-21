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

enum class FuturesStatus(val label: String, val description: String) {
    AVAILABLE("Dostępne", "Funkcje obecne w tej wersji aplikacji. Dostępność nie oznacza braku ograniczeń."),
    IN_DEVELOPMENT("W rozwoju", "Zatwierdzone prace, których realizacja została potwierdzona."),
    PLANNED("Planowane", "Przyszłe prace wyraźnie zaakceptowane przez właściciela produktu."),
    UNDER_REVIEW("Do oceny", "Propozycje lub luki wymagające oceny. Nie oznaczają zatwierdzenia wdrożenia.")
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
        summary = "Odczyt glikemii, trend i informacja o aktualności danych na ekranie głównym.",
        status = FuturesStatus.AVAILABLE,
        audiences = FuturesAudience.entries.toSet(),
        nowValue = "Ekran monitorowania pokazuje ostatni dostępny odczyt. Sprawdź jego czas — synchronizacja może być opóźniona.",
        highlights = listOf(
            "Wskaźnik aktualności danych i kolorowe ostrzeżenia dla użytkownika.",
            "Szybki przegląd wartości, trendu i zakresu docelowego.",
            "Wspólna podstawa dla ekranu głównego, opiekuna i analityki."
        ),
        nextStep = "Otwórz ekran główny, aby sprawdzić odczyt i jego aktualność."
    ),
    FutureIdea(
        id = "history-and-chart",
        emoji = "📊",
        title = "Historia i interaktywny wykres",
        summary = "Przegląd zapisanej historii glikemii, wybór okresu i powiększanie wykresu w widoku historii.",
        status = FuturesStatus.AVAILABLE,
        audiences = FuturesAudience.entries.toSet(),
        nowValue = "Historia korzysta z dostępnych zapisów. Brak odczytów i ustawiona retencja ograniczają widoczny okres.",
        highlights = listOf(
            "Po naciśnięciu wykres otwiera się w pełnym widoku historii.",
            "Widok historii pozwala wybierać okres i odczyt na wykresie.",
            "Wykres oznacza minimum, maksimum i zakres docelowy."
        ),
        nextStep = "Otwórz historię z wykresu na ekranie głównym."
    ),
    FutureIdea(
        id = "libreview-csv-import",
        emoji = "📥",
        title = "Import LibreView CSV",
        summary = "Wczytywanie pliku lub folderu CSV z LibreView, podgląd i przypisanie danych do wybranej osoby.",
        status = FuturesStatus.AVAILABLE,
        audiences = FuturesAudience.entries.toSet(),
        nowValue = "Import wymaga podglądu i potwierdzenia osoby. Obsługiwane formaty i strefa czasowa wpływają na wynik odczytu pliku.",
        highlights = listOf(
            "Wybór pliku lub folderu przez systemowy selektor dokumentów.",
            "Podgląd nowych rekordów, duplikatów i konfliktów przed zapisem.",
            "Dane historyczne nie zastępują bieżących odczytów czujnika."
        ),
        nextStep = "Import znajdziesz w ustawieniach, w sekcji Prywatność i dane."
    ),
    FutureIdea(
        id = "analytics-and-statistics",
        emoji = "🧮",
        title = "Analizy i statystyki",
        summary = "Miary, TIR, średnie, rozkłady i statystyki dla wybranego zakresu czasu z czytelnym podsumowaniem użytkownika.",
        status = FuturesStatus.AVAILABLE,
        audiences = FuturesAudience.entries.toSet(),
        nowValue = "Analiza pokazuje metryki dla dostępnych danych i wybranego okresu. Wyniki nie zastępują oceny medycznej.",
        highlights = listOf(
            "Wizualizacja zakresu czasu i trendu w analizie.",
            "Zestaw metryk i istotnych wartości dla użytkownika i opiekuna.",
            "Pokrycie danych i wybrany okres należy uwzględnić przy interpretacji."
        ),
        nextStep = "Otwórz analizę z dolnego paska nawigacji. Przegląd dokładności 12h metryk pozostaje osobnym zadaniem."
    ),
    FutureIdea(
        id = "person-profiles",
        emoji = "👥",
        title = "Profile obserwowanych osób",
        summary = "Wybór obserwowanej osoby i prezentacja jej odczytów oraz statusu danych.",
        status = FuturesStatus.AVAILABLE,
        audiences = setOf(FuturesAudience.OPIEKUN, FuturesAudience.PACJENT, FuturesAudience.LEKARZ),
        nowValue = "W aktualnym produkcie istnieje wybór osób, osobna logika podziału na sekcje oraz osobne karty monitorowanych profili.",
        highlights = listOf(
            "Wybór osoby z obsługą braku dostępnych danych.",
            "Wysokiej wagi sekcja \"Wymaga uwagi\" dla opiekuna.",
            "Przejrzysta hierarchia profili i statusu danych."
        ),
        nextStep = "Wybierz osobę na ekranie głównym i sprawdź jej status danych."
    ),
    FutureIdea(
        id = "privacy-and-data",
        emoji = "🔒",
        title = "Prywatność i dane",
        summary = "Ustawienia retencji, usuwania danych i diagnostyki oraz informacje o wykorzystaniu pamięci i transferu.",
        status = FuturesStatus.AVAILABLE,
        audiences = FuturesAudience.entries.toSet(),
        nowValue = "Ustawienia udostępniają kontrolę danych. Obecność tych opcji nie jest gwarancją integralności kopii zapasowej.",
        highlights = listOf(
            "Prywatność i dane są rozdzielone w ustawieniach aplikacji.",
            "Przechowywanie i retencja są dostępne w istniejącym produkcie.",
            "Użytkownik ma kontrolę nad ustawieniami danych i diagnostyki."
        ),
        nextStep = "Otwórz ustawienia. Przed usunięciem danych sprawdź możliwość ich odzyskania."
    ),
    FutureIdea(
        id = "backup-and-restore",
        emoji = "💾",
        title = "Kopie i odzyskiwanie danych",
        summary = "Istnieją opcje kopii i odtwarzania, ale kompletność odzyskiwania danych wymaga weryfikacji.",
        status = FuturesStatus.UNDER_REVIEW,
        audiences = setOf(FuturesAudience.OPIEKUN, FuturesAudience.PACJENT, FuturesAudience.LEKARZ),
        nowValue = "Mechanizm kopii istnieje. Otwarte luki integralności nie pozwalają traktować go jako gwarancji odzyskania danych. Nie potwierdzono aktywnego, zatwierdzonego wdrożenia poprawek.",
        highlights = listOf(
            "Opcje kopii zapasowych znajdują się w ustawieniach danych.",
            "Weryfikacja integralności i odtwarzania pozostaje priorytetem.",
            "Niezbędne są dalsze testy przy poprawnie przechowywanych i odzyskiwanych danych."
        ),
        nextStep = "Potrzebne są testy odtwarzania i osobna decyzja właściciela produktu o zakresie poprawek."
    ),
    FutureIdea(
        id = "caregiver-senior-clinician-modes",
        emoji = "🧭",
        title = "Tryby Opiekun / Senior / Lekarz",
        summary = "Jedna aplikacja z wieloma perspektywami użytkownika, z bieżącym głównym kierunkiem opiekuna i kolejnymi etapami dla innych ról.",
        status = FuturesStatus.UNDER_REVIEW,
        audiences = FuturesAudience.entries.toSet(),
        nowValue = "Kierunek trzech perspektyw jest zaakceptowany w dokumentacji. Nie oznacza to aktywnego wdrożenia pełnych trybów ani trwałego przełączania między nimi.",
        highlights = listOf(
            "Obecny produkt jest zorientowany na główny tryb opiekuna.",
            "Docelowe zasady prezentacji opisano w dokumentacji produktu.",
            "Nie ma jeszcze pełnej, produkcyjnej implementacji wszystkich trybów."
        ),
        nextStep = "Zakres wdrożenia i zapamiętywania preferencji wymaga osobnej akceptacji właściciela produktu. Filtr na tej stronie nie zmienia trybu aplikacji."
    ),
    FutureIdea(
        id = "alerts-and-refresh",
        emoji = "🔔",
        title = "Alerty i odświeżanie danych",
        summary = "Sygnały ostrzegawcze, regularne odświeżanie danych i spójna prezentacja świeżości w całym interfejsie użytkownika.",
        status = FuturesStatus.UNDER_REVIEW,
        audiences = setOf(FuturesAudience.OPIEKUN, FuturesAudience.PACJENT, FuturesAudience.LEKARZ),
        nowValue = "Odświeżanie i powiadomienia istnieją. Zaakceptowano analizę alarmów, lecz zmiany ich działania pozostają wstrzymane do osobnej decyzji właściciela produktu.",
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
        nextStep = "Najpierw porównanie metryk z danymi źródłowymi i decyzja o zakresie ewentualnej poprawki; nowe badanie użytkowników nie jest warunkiem takiego testu."
    )
)

