package com.libredisplay.ui.monitoring

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libredisplay.ui.theme.LibreCareColors

internal enum class DashboardNavItem(val label: String, val accessibilityLabel: String) {
    GLOWNA("Główna", "Główna"),
    HISTORIA("Analiza", "Analiza"),
    USTAWIENIA("Ustawienia", "Ustawienia")
}

@Composable
internal fun TopLevelNavigationBar(
    selected: DashboardNavItem,
    onOpenHome: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit
) {
    NavigationBar(
        containerColor = LibreCareColors.SurfaceElevated,
        tonalElevation = 0.dp,
        modifier = Modifier.heightIn(min = 68.dp)
    ) {
        TopLevelNavItem(selected == DashboardNavItem.GLOWNA, DashboardNavItem.GLOWNA.label, Icons.Default.Home, onOpenHome)
        TopLevelNavItem(selected == DashboardNavItem.HISTORIA, DashboardNavItem.HISTORIA.label, Icons.Filled.BarChart, onOpenHistory)
        TopLevelNavItem(selected == DashboardNavItem.USTAWIENIA, DashboardNavItem.USTAWIENIA.label, Icons.Default.Settings, onOpenSettings)
    }
}

@Composable
private fun RowScope.TopLevelNavItem(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .weight(1f)
            .testTag(LibreCareTestTags.bottomNav(DashboardNavItem.entries.first { it.label == label }))
            .semantics { this.selected = selected }
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) LibreCareColors.AccentTeal else LibreCareColors.TextSecondary,
            modifier = Modifier.size(22.dp)
        )
        Text(
            label,
            fontSize = 12.sp,
            color = if (selected) LibreCareColors.TextPrimary else LibreCareColors.TextSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/**
 * Landscape counterpart of [TopLevelNavigationBar]: the same destinations rendered as a vertical
 * rail anchored to the right edge, so landscape frees the top and bottom of the screen for content.
 */
@Composable
internal fun SideNavigationRail(
    selected: DashboardNavItem,
    onOpenHome: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(76.dp)
            .background(LibreCareColors.SurfaceElevated)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SideNavItem(selected == DashboardNavItem.GLOWNA, DashboardNavItem.GLOWNA.label, Icons.Default.Home, onOpenHome)
        SideNavItem(selected == DashboardNavItem.HISTORIA, DashboardNavItem.HISTORIA.label, Icons.Filled.BarChart, onOpenHistory)
        SideNavItem(selected == DashboardNavItem.USTAWIENIA, DashboardNavItem.USTAWIENIA.label, Icons.Default.Settings, onOpenSettings)
    }
}

@Composable
private fun SideNavItem(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .testTag(LibreCareTestTags.bottomNav(DashboardNavItem.entries.first { it.label == label }))
            .semantics { this.selected = selected }
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (selected) LibreCareColors.AccentTeal else LibreCareColors.TextSecondary,
            modifier = Modifier.size(24.dp)
        )
        Text(
            label,
            fontSize = 12.sp,
            color = if (selected) LibreCareColors.TextPrimary else LibreCareColors.TextSecondary,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

