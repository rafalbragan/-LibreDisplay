package com.libredisplay.ui.futures

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.libredisplay.ui.monitoring.DashboardNavItem
import com.libredisplay.ui.monitoring.LibreCareTestTags
import com.libredisplay.ui.monitoring.TopLevelNavigationBar
import com.libredisplay.ui.theme.LibreCareColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuturesScreen(
    onOpenHome: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: FuturesViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Futures")
                        Text(
                            "Prototypy funkcji i kierunki rozwoju",
                            color = LibreCareColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            )
        },
        bottomBar = {
            TopLevelNavigationBar(
                selected = DashboardNavItem.FUTURES,
                onOpenHome = onOpenHome,
                onOpenHistory = onOpenAnalytics,
                onOpenFutures = {},
                onOpenSettings = onOpenSettings
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
                .testTag(LibreCareTestTags.FUTURES_SCREEN),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FuturesHeroCard()
            AudienceSelector(
                selected = state.selectedAudience,
                onSelect = viewModel::selectAudience
            )
            FuturesSectionHeader(
                title = "Najbliższe do wdrożenia",
                subtitle = "To są kierunki, które już mają opis techniczny i mogą wejść po akceptacji."
            )
            state.visibleIdeas.filter { it.status == FuturesStatus.GOTOWE_DO_TESTU }.forEach { idea ->
                FutureIdeaCard(
                    idea = idea,
                    expanded = idea.id in state.expandedIdeaIds,
                    onToggle = { viewModel.toggleIdea(idea.id) }
                )
            }

            FuturesSectionHeader(
                title = "Eksperymenty produktowe",
                subtitle = "Pomysły dla pacjenta, opiekuna, seniora i lekarza, które warto obejrzeć przed wdrożeniem."
            )
            state.visibleIdeas.filter { it.status == FuturesStatus.PROTOTYP }.forEach { idea ->
                FutureIdeaCard(
                    idea = idea,
                    expanded = idea.id in state.expandedIdeaIds,
                    onToggle = { viewModel.toggleIdea(idea.id) }
                )
            }

            FuturesSectionHeader(
                title = "Zależności i dane",
                subtitle = "Te kierunki mają potencjał, ale wymagają dodatkowych danych albo oddzielnej infrastruktury."
            )
            state.visibleIdeas.filter { it.status != FuturesStatus.GOTOWE_DO_TESTU && it.status != FuturesStatus.PROTOTYP }.forEach { idea ->
                FutureIdeaCard(
                    idea = idea,
                    expanded = idea.id in state.expandedIdeaIds,
                    onToggle = { viewModel.toggleIdea(idea.id) }
                )
            }

            FuturesRoadmapCard()
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FuturesHeroCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LibreCareColors.Surface.copy(alpha = 0.32f), RoundedCornerShape(14.dp))
            .border(1.dp, LibreCareColors.Surface, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = LibreCareColors.AccentTeal)
            Text(
                text = "Bezpieczna strefa prototypów",
                color = LibreCareColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
        Text(
            text = "Tutaj zbieramy to, co warto przetestować przed zmianami na ekranie głównym, w statystykach i w przyszłych widokach dla seniora, opiekuna oraz lekarza.",
            color = LibreCareColors.TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Text(
            text = "Obecna funkcjonalność Monitoringu, Analizy i Ustawień pozostaje bez zmian — zakładka Futures ma pokazać kierunek, priorytety i wartość dla użytkownika.",
            color = LibreCareColors.TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun AudienceSelector(
    selected: FuturesAudience,
    onSelect: (FuturesAudience) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Filtr perspektywy",
            color = LibreCareColors.TextPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FuturesAudience.entries.forEach { audience ->
                AudienceChip(
                    tag = LibreCareTestTags.futuresAudience(audience.name.lowercase()),
                    label = audience.label,
                    selected = audience == selected,
                    onClick = { onSelect(audience) }
                )
            }
        }
    }
}

@Composable
private fun AudienceChip(
    tag: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                if (selected) LibreCareColors.AccentTeal.copy(alpha = 0.18f) else LibreCareColors.Surface.copy(alpha = 0.3f),
                RoundedCornerShape(999.dp)
            )
            .border(1.dp, if (selected) LibreCareColors.AccentTeal else LibreCareColors.Surface, RoundedCornerShape(999.dp))
            .testTag(tag)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = LibreCareColors.TextPrimary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun FuturesSectionHeader(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            color = LibreCareColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = subtitle,
            color = LibreCareColors.TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun FutureIdeaCard(
    idea: FutureIdea,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LibreCareColors.Surface.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
            .border(1.dp, LibreCareColors.Surface, RoundedCornerShape(14.dp))
            .padding(14.dp)
            .testTag(LibreCareTestTags.futuresCard(idea.id)),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "${idea.emoji} ${idea.title}",
                    color = LibreCareColors.TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = idea.summary,
                    color = LibreCareColors.TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(status = idea.status)
                    idea.audiences
                        .filter { it != FuturesAudience.WSZYSCY }
                        .take(3)
                        .forEach { audience ->
                            MiniBadge(label = audience.label)
                        }
                }
            }
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Ukryj szczegóły" else "Pokaż szczegóły",
                    tint = LibreCareColors.TextSecondary
                )
            }
        }

        InfoBlock(title = "Po co", body = idea.nowValue)

        if (expanded) {
            HorizontalDivider(color = LibreCareColors.SurfaceMuted)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Co pokazujemy w Futures",
                    color = LibreCareColors.TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                idea.highlights.forEach { bullet ->
                    Text(
                        text = "• $bullet",
                        color = LibreCareColors.TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
            InfoBlock(title = "Następny krok", body = idea.nextStep)
        }
    }
}

@Composable
private fun InfoBlock(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            color = LibreCareColors.TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = body,
            color = LibreCareColors.TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun StatusBadge(status: FuturesStatus) {
    val color = when (status) {
        FuturesStatus.GOTOWE_DO_TESTU -> LibreCareColors.AccentTeal
        FuturesStatus.PROTOTYP -> LibreCareColors.AccentAmber
        FuturesStatus.WYMAGA_DANYCH -> LibreCareColors.AccentRed.copy(alpha = 0.85f)
        FuturesStatus.WYMAGA_BACKENDU -> LibreCareColors.TextSecondary
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
            .border(1.dp, color, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = status.label,
            color = color,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun MiniBadge(label: String) {
    Box(
        modifier = Modifier
            .background(LibreCareColors.Surface.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = LibreCareColors.TextSecondary,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun FuturesRoadmapCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(LibreCareColors.Surface.copy(alpha = 0.26f), RoundedCornerShape(14.dp))
            .border(1.dp, LibreCareColors.Surface, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Roadmapa decyzji",
            color = LibreCareColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = "1. Obejrzeć Futures i ocenić priorytety.\n2. Wybrać, co przenieść na istniejący ekran Analiza.\n3. Dopiero potem zdecydować, co ma trafić na Główną, do Statystyk albo do widoków Senior / Opiekun / Lekarz.",
            color = LibreCareColors.TextSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
        Text(
            text = "Dokumentacja techniczna została przygotowana w katalogu docs/, więc wdrożenie można rozbić na bezpieczne etapy.",
            color = LibreCareColors.TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}


