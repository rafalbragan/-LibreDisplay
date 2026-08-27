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
    PROTOTYP("Prototyp"),
    GOTOWE_DO_TESTU("Gotowe do testu"),
    WYMAGA_DANYCH("Wymaga danych"),
    WYMAGA_BACKENDU("Wymaga backendu")
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
    val expandedIdeaIds: Set<String> = setOf("analysis-prototype"),
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
        id = "analysis-prototype",
        emoji = "📊",
        title = "Analiza+ do wdrożenia",
        summary = "Prototyp zmian dla ekranu Analiza bez ruszania jeszcze obecnego przepływu.",
        status = FuturesStatus.GOTOWE_DO_TESTU,
        audiences = setOf(FuturesAudience.PACJENT, FuturesAudience.OPIEKUN, FuturesAudience.LEKARZ, FuturesAudience.SENIOR),
        nowValue = "Najbliższy kandydat do wdrożenia: większe wnioski, zakres 1-90 dni, szybsza nawigacja, spięty wykres i przyklejona kolumna metryk.",
        highlights = listOf(
            "Wnioski/obserwacje: większa czcionka i lepsza czytelność.",
            "Okres analizy: wpisanie dowolnej liczby dni od 1 do 90.",
            "Przewijanie o dzień, tydzień i miesiąc bez gubienia kontekstu.",
            "Wykres średniej: wyraźniejszy kolor i grubsza linia.",
            "Metryki: opisy po lewej pozostają stale widoczne podczas przewijania poziomego."
        ),
        nextStep = "Jeśli prototyp układu i priorytetów jest trafny, to ten pakiet warto wdrażać jako pierwszy."
    ),
    FutureIdea(
        id = "spike-explanations",
        emoji = "🧠",
        title = "Wyjaśnienia skoków glikemii",
        summary = "Ekran tłumaczący możliwe przyczyny wzrostów i spadków zamiast zostawiania użytkownika z samą liczbą.",
        status = FuturesStatus.PROTOTYP,
        audiences = setOf(FuturesAudience.PACJENT, FuturesAudience.OPIEKUN, FuturesAudience.LEKARZ),
        nowValue = "Lekarze i pacjenci zwykle pytają: dlaczego był skok o 15:30? Tego dziś aplikacja nie tłumaczy.",
        highlights = listOf(
            "Ranking możliwych przyczyn: posiłek, stres, aktywność, choroba, hormony.",
            "Krótka rekomendacja: co sprawdzić przy następnym podobnym zdarzeniu.",
            "Możliwość późniejszego spięcia z wpisami o posiłkach i aktywności.",
            "Przydatne dla rozmowy pacjent-opiekun-lekarz."
        ),
        nextStep = "Najpierw warto zebrać komentarze, jakie wyjaśnienia byłyby naprawdę pomocne i czy mają być ostrożne, czy bardziej konkretne."
    ),
    FutureIdea(
        id = "hypo-risk",
        emoji = "⚠️",
        title = "Ryzyko hipoglikemii",
        summary = "Widok prognozujący, kiedy i dlaczego ryzyko niskich cukrów jest największe.",
        status = FuturesStatus.PROTOTYP,
        audiences = setOf(FuturesAudience.PACJENT, FuturesAudience.OPIEKUN, FuturesAudience.LEKARZ, FuturesAudience.SENIOR),
        nowValue = "Obecnie widzimy historię. Brakuje prostego komunikatu: na co uważać w kolejnych dniach.",
        highlights = listOf(
            "Pokazanie pór dnia o najwyższym ryzyku.",
            "Trend ryzyka: maleje / rośnie / stabilne.",
            "Krótki opis: dlaczego sygnał ostrzegawczy się pojawił.",
            "Bez podejmowania decyzji medycznych za użytkownika."
        ),
        nextStep = "Dobry kandydat do wersji dla seniora i opiekuna, jeśli komunikat będzie prosty i nie będzie straszył nadmiernie."
    ),
    FutureIdea(
        id = "variability-patterns",
        emoji = "📈",
        title = "Wzorce zmienności",
        summary = "Zamiast samego CV: wskazanie kiedy w ciągu dnia i tygodnia glikemia jest najmniej stabilna.",
        status = FuturesStatus.GOTOWE_DO_TESTU,
        audiences = setOf(FuturesAudience.LEKARZ, FuturesAudience.OPIEKUN, FuturesAudience.PACJENT),
        nowValue = "To odpowiada na pytanie lekarza: gdzie jest największy problem — rano, po południu, w weekend, w nocy?",
        highlights = listOf(
            "Porównanie rano / dzień / wieczór / noc.",
            "Porównanie dni tygodnia i weekendu.",
            "Interpretacja po polsku, nie tylko wartości liczbowe.",
            "Świetne uzupełnienie obecnej Analizy."
        ),
        nextStep = "Można zacząć od prostych reguł i dopiero później rozszerzać o bardziej zaawansowaną interpretację."
    ),
    FutureIdea(
        id = "meal-response",
        emoji = "🍽️",
        title = "Reakcja na posiłki",
        summary = "Podsumowanie jak różne posiłki wpływają na szczyt glikemii i czas powrotu do normy.",
        status = FuturesStatus.WYMAGA_DANYCH,
        audiences = setOf(FuturesAudience.PACJENT, FuturesAudience.OPIEKUN, FuturesAudience.LEKARZ),
        nowValue = "Ta funkcja ma duży sens, ale wymaga wiarygodnych danych o posiłkach lub ich sensownego wprowadzania.",
        highlights = listOf(
            "Średni szczyt po posiłku i czas do powrotu.",
            "Najlepsze i najtrudniejsze posiłki użytkownika.",
            "Materiał do rozmowy z dietetykiem lub lekarzem.",
            "Duży potencjał, ale trzeba uważać na jakość danych wejściowych."
        ),
        nextStep = "Najpierw sprawdzić, jaki minimalny model wpisu posiłku nie będzie uciążliwy dla użytkownika."
    ),
    FutureIdea(
        id = "weekly-report",
        emoji = "📋",
        title = "Tygodniowa karta postępów",
        summary = "Jedna karta z odpowiedzią: jak mi poszedł tydzień i na czym mam się skupić dalej.",
        status = FuturesStatus.GOTOWE_DO_TESTU,
        audiences = setOf(FuturesAudience.PACJENT, FuturesAudience.OPIEKUN, FuturesAudience.SENIOR),
        nowValue = "To może być bardzo mocne dla codziennego użytkownika, bo zamienia wiele wykresów w prostą narrację.",
        highlights = listOf(
            "Ocena tygodnia z krótkim komentarzem.",
            "Najlepsze dni i najsłabsze momenty.",
            "2-4 konkretne wskazówki bez przeciążania informacją.",
            "Dobry materiał do ekranu senior lub opiekun."
        ),
        nextStep = "Można wdrażać jako osobną kartę bez ingerencji w obecny ekran główny."
    ),
    FutureIdea(
        id = "sensor-wear",
        emoji = "⏱️",
        title = "Aktywność sensora i luki danych",
        summary = "Widok pokazujący, czy dane są kompletne oraz kiedy pojawiają się większe przerwy.",
        status = FuturesStatus.PROTOTYP,
        audiences = setOf(FuturesAudience.OPIEKUN, FuturesAudience.LEKARZ, FuturesAudience.SENIOR),
        nowValue = "To pomaga szybko odróżnić problem medyczny od problemu z danymi lub noszeniem sensora.",
        highlights = listOf(
            "Aktywność sensora w procentach i lista większych luk.",
            "Wskazanie czy luka jest nocna, krótka czy nietypowa.",
            "Naturalne uzupełnienie wskaźnika świeżości danych na ekranie głównym.",
            "Przydatne przy rozmowie: dlaczego wykres wygląda inaczej niż zwykle."
        ),
        nextStep = "Warto połączyć to kiedyś z prostym komunikatem na Głównej, ale bez zmian w obecnym ekranie na tym etapie."
    ),
    FutureIdea(
        id = "achievements",
        emoji = "🏆",
        title = "Osiągnięcia i serie",
        summary = "Lekka gamifikacja dla użytkowników, którzy potrzebują motywacji, nie kolejnych tabelek.",
        status = FuturesStatus.PROTOTYP,
        audiences = setOf(FuturesAudience.PACJENT, FuturesAudience.SENIOR),
        nowValue = "To nie jest funkcja kliniczna, ale może zwiększyć regularność i zaangażowanie.",
        highlights = listOf(
            "Serie dni z dobrym TIR lub bez nocnych spadków.",
            "Delikatna motywacja bez oceniania i bez presji.",
            "Możliwość ukrycia dla użytkowników, którzy wolą czysto medyczny interfejs."
        ),
        nextStep = "Testować dopiero po ocenie czy grupa docelowa chce elementy motywacyjne."
    ),
    FutureIdea(
        id = "share-doctor",
        emoji = "📤",
        title = "Udostępnij lekarzowi",
        summary = "Skrócony raport lub kod QR do konsultacji zamiast wielu zrzutów ekranu.",
        status = FuturesStatus.WYMAGA_BACKENDU,
        audiences = setOf(FuturesAudience.PACJENT, FuturesAudience.OPIEKUN, FuturesAudience.LEKARZ),
        nowValue = "Wielu użytkowników potrzebuje łatwego przekazania danych do lekarza, ale bez przesady i bez chaosu.",
        highlights = listOf(
            "Skrócony raport za wybrany okres.",
            "Komentarz pacjenta lub opiekuna do wizyty.",
            "Potencjalny kod QR lub bezpieczny link czasowy.",
            "Wymaga osobnego przeglądu prywatności i zakresu danych."
        ),
        nextStep = "Najpierw ustalić minimalny, bezpieczny zakres udostępnianych informacji."
    ),
    FutureIdea(
        id = "role-screens",
        emoji = "🧭",
        title = "Ekrany Senior / Opiekun / Lekarz",
        summary = "Różne role potrzebują innych priorytetów: spokoju, szybkich alarmów lub przeglądu klinicznego.",
        status = FuturesStatus.PROTOTYP,
        audiences = setOf(FuturesAudience.WSZYSCY, FuturesAudience.SENIOR, FuturesAudience.OPIEKUN, FuturesAudience.LEKARZ),
        nowValue = "To może stać się najważniejszym wyróżnikiem produktu, jeśli nie będziemy każdemu pokazywać tego samego interfejsu.",
        highlights = listOf(
            "Senior: większe litery, prostszy język, mniej elementów naraz.",
            "Opiekun: szybki status, świeżość danych, ryzyko i kontakt.",
            "Lekarz: trendy, zmienność, eksport, przygotowanie do wizyty.",
            "Możliwe bez zmiany package/applicationId i bez naruszania bieżących ekranów."
        ),
        nextStep = "Najpierw zebrać opinie w zakładce Futures, a dopiero później wyodrębniać nowe docelowe ekrany."
    )
)


