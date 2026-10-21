package com.libredisplay.ui.futures

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.libredisplay.ui.monitoring.DashboardNavItem
import com.libredisplay.ui.monitoring.LibreCareTestTags
import com.libredisplay.ui.monitoring.TopLevelNavigationBar
import com.libredisplay.ui.theme.LibreCareColors

@Composable
fun FuturesScreen(
    onOpenHome: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    viewModel: FuturesViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    FuturesContent(state, viewModel::selectAudience, viewModel::toggleIdea, onOpenHome, onOpenAnalytics, onOpenSettings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FuturesContent(
    state: FuturesUiState,
    onSelectAudience: (FuturesAudience) -> Unit,
    onToggleIdea: (String) -> Unit,
    onOpenHome: () -> Unit = {},
    onOpenAnalytics: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    Scaffold(
        containerColor = LibreCareColors.Background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = LibreCareColors.Background,
                    titleContentColor = LibreCareColors.TextPrimary
                ),
                title = {
                    Column {
                        Text("Funkcje LibreCare")
                        Text(
                            "Dostępność i propozycje rozwoju",
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
                onSelect = onSelectAudience
            )
            if (state.visibleIdeas.isEmpty()) {
                Text("Brak funkcji dla wybranej perspektywy. Wybierz filtr Wszyscy.", color = LibreCareColors.TextPrimary)
            }
            FuturesStatus.entries.forEach { status ->
                val ideas = state.visibleIdeas.filter { it.status == status }
                if (ideas.isNotEmpty()) {
                    FuturesSectionHeader(title = status.label, subtitle = status.description)
                    ideas.forEach { idea ->
                        FutureIdeaCard(
                            idea = idea,
                            expanded = idea.id in state.expandedIdeaIds,
                            onToggle = { onToggleIdea(idea.id) }
                        )
                    }
                }
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
                text = "Co jest dostępne, a co wymaga decyzji",
                color = LibreCareColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }
        Text(
            text = "Lista opisuje funkcje tej wersji LibreCare oraz otwarte propozycje. Nie potwierdza publikacji w Google Play ani niezawodności medycznej.",
            color = LibreCareColors.TextSecondary,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Text(
            text = "Dostępne ekrany otworzysz z dolnego paska. Propozycja nie jest obietnicą wdrożenia ani poradą dotyczącą leczenia.",
            color = LibreCareColors.TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
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
        Text(
            text = "Filtr zmienia tylko tę listę, nie tryb aplikacji.",
            color = LibreCareColors.TextSecondary,
            fontSize = 13.sp
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
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

@OptIn(ExperimentalLayoutApi::class)
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
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StatusBadge(status = idea.status)
                    idea.audiences
                        .filter { it != FuturesAudience.WSZYSCY }
                        .forEach { audience ->
                            MiniBadge(label = audience.label)
                        }
                }
            }
            IconButton(onClick = onToggle, modifier = Modifier.testTag("futures_toggle_${idea.id}")) {
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = "${if (expanded) "Ukryj" else "Pokaż"} szczegóły: ${idea.title}",
                    tint = LibreCareColors.TextSecondary
                )
            }
        }

        InfoBlock(title = "Stan i ograniczenia", body = idea.nowValue)

        if (expanded) {
            HorizontalDivider(color = LibreCareColors.SurfaceMuted)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Szczegóły",
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
        FuturesStatus.AVAILABLE -> LibreCareColors.AccentTeal
        FuturesStatus.IN_DEVELOPMENT -> LibreCareColors.AccentAmber
        FuturesStatus.PLANNED -> LibreCareColors.TextPrimary
        FuturesStatus.UNDER_REVIEW -> LibreCareColors.TextSecondary
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
            text = "Jak podejmowane są decyzje",
            color = LibreCareColors.TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = "Planowane oznacza akceptację właściciela produktu. W rozwoju wymaga także potwierdzenia aktywnych prac. Do oceny nie oznacza zgody na wdrożenie.",
            color = LibreCareColors.TextSecondary,
            fontSize = 13.sp,
            lineHeight = 19.sp
        )
        Text(
            text = "Wcześniejsze pomysły zachowano w dokumentacji produktu. Ich obecność na liście propozycji nie zmienia zatwierdzonych wymagań.",
            color = LibreCareColors.TextSecondary,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}


