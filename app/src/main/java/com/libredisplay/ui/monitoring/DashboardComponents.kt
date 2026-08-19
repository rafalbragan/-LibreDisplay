package com.libredisplay.ui.monitoring

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.libredisplay.R
import com.libredisplay.data.model.LibreConnectionPerson

private val DashboardBackground = Color(0xFF101318)
private val DashboardSurface = Color(0xFF182033)
private val DashboardElevatedSurface = Color(0xFF202A3D)
private val DashboardPrimaryText = Color(0xFFF3F6FA)
private val DashboardSecondaryText = Color(0xFFAAB3C2)
private val AccentGreen = Color(0xFF43C59E)
private val AccentWarning = Color(0xFFF2B84B)
private val AccentRed = Color(0xFFE05A6A)

/**
 * Single compact person switcher area.
 * This is the only place where selected monitored person identity is displayed.
 */
@Composable
fun CompactPersonSwitcherBar(
    persons: List<LibreConnectionPerson>,
    selectedPatientId: String?,
    onPersonSelected: (String) -> Unit,
    isDemoMode: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        VisiblePersonSwitcher(
            persons = persons,
            selectedPatientId = selectedPatientId,
            onPersonSelected = onPersonSelected,
            modifier = Modifier.weight(1f)
        )
        if (isDemoMode) {
            Surface(
                color = AccentWarning.copy(alpha = 0.16f),
                shape = RoundedCornerShape(999.dp)
            ) {
                Text(
                    text = "DEMO",
                    color = AccentWarning,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }
        }
    }
}

/**
 * Visible person switcher using chips/buttons instead of dropdown.
 * Shows up to 3 people as selectable chips. For more than 3, use horizontal scroll.
 */
@Composable
fun VisiblePersonSwitcher(
    persons: List<LibreConnectionPerson>,
    selectedPatientId: String?,
    onPersonSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (persons.isEmpty()) {
        Surface(
            color = DashboardElevatedSurface,
            shape = RoundedCornerShape(12.dp),
            modifier = modifier.heightIn(min = 40.dp)
        ) {
            Text(
                text = stringResource(R.string.no_persons),
                color = DashboardSecondaryText,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
            )
        }
        return
    }

    val isScrollable = persons.size > 3

    Box(
        modifier = modifier
            .fillMaxWidth()
    ) {
        if (isScrollable) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                persons.forEach { person ->
                    PersonChip(
                        name = person.firstName + (if (person.lastName != null) " ${person.lastName}" else ""),
                        isSelected = person.patientId == selectedPatientId,
                        onClick = { onPersonSelected(person.patientId) }
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                persons.forEach { person ->
                    PersonChip(
                        name = person.firstName + (if (person.lastName != null) " ${person.lastName}" else ""),
                        isSelected = person.patientId == selectedPatientId,
                        onClick = { onPersonSelected(person.patientId) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = if (isSelected) AccentGreen.copy(alpha = 0.18f) else DashboardElevatedSurface,
        shape = RoundedCornerShape(999.dp),
        modifier = modifier
            .clickable(enabled = !isSelected) { onClick() }
            .heightIn(min = 36.dp)
    ) {
        Text(
            text = name,
            color = if (isSelected) AccentGreen else DashboardSecondaryText,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Compact statistics grid with 2-3 columns and minimal padding.
 * Shows key metrics without dominating the dashboard.
 */
@Composable
fun CompactStatisticsGrid(
    belowRange: String,
    inRange: String,
    aboveRange: String,
    sensorActivity: String? = null,
    averageGlucose: String? = null,
    gmiValue: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Row 1: Below, In Range, Above
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CompactStatBox("Poniżej", belowRange, modifier = Modifier.weight(1f))
            CompactStatBox("W zakresie", inRange, AccentGreen, modifier = Modifier.weight(1f))
            CompactStatBox("Powyżej", aboveRange, modifier = Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(4.dp))
        // Row 2: Sensor, Average, GMI
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (sensorActivity != null) {
                CompactStatBox("Sensor", sensorActivity, modifier = Modifier.weight(1f))
            }
            if (averageGlucose != null) {
                CompactStatBox("Średnia", averageGlucose, modifier = Modifier.weight(1f))
            }
            if (gmiValue != null) {
                CompactStatBox("GMI", gmiValue, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CompactStatBox(
    label: String,
    value: String,
    accentColor: Color = AccentWarning,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DashboardSurface),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier.height(60.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                label,
                color = DashboardSecondaryText,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                value,
                color = accentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Time range display showing selected period clearly.
 * Flat design – no Surface/pill wrapper. Just a row with subtle separator.
 */
@Composable
fun TimeRangeDisplay(
    timeRange: TimeRangeState,
    latestReadingAt: java.time.Instant?,
    onChangeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.AccessTime, contentDescription = null, tint = DashboardSecondaryText)
        Text(
            text = compactDashboardRangeLabel(timeRange, latestReadingAt),
            color = DashboardSecondaryText,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onChangeClick) {
            Text(text = "Zmień", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}


