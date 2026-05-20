package com.otakeessen.underpressure.ui.table.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otakeessen.underpressure.R
import com.otakeessen.underpressure.domain.BloodPressureClassifier
import com.otakeessen.underpressure.domain.BloodPressureLevel
import com.otakeessen.underpressure.domain.BpGuidelines
import com.otakeessen.underpressure.domain.TrackerDefinition
import com.otakeessen.underpressure.domain.TrackerType
import com.otakeessen.underpressure.domain.TrackerValue
import com.otakeessen.underpressure.ui.table.DayMeasurementSummary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * A single row in the measurement table showing multiple slots.
 */
@Composable
fun DayRow(
    summary: DayMeasurementSummary,
    slotCount: Int,
    guidelines: BpGuidelines,
    activeTrackers: List<TrackerDefinition> = emptyList(),
    onCellClick: (slotIndex: Int) -> Unit,
    isSummaryVisible: Boolean,
    modifier: Modifier = Modifier
) {
    val date = remember(summary.date) { LocalDate.parse(summary.date) }
    val isWeekend = date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY

    // 1. Dynamic Font Size Calculation
    val measurementFontSize = when {
        slotCount <= 2 -> 16.sp
        slotCount <= 3 -> 14.sp
        else -> 12.sp
    }

    // 2. Format date for better space utilization (e.g., "21 Mar, Sat")
    val displayDate = remember(summary.date) {
        val formatter = DateTimeFormatter.ofPattern("dd MMM, EE")
        date.format(formatter)
    }

    val backgroundColor = when {
        summary.isToday -> MaterialTheme.colorScheme.secondaryContainer
        isWeekend -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surface
    }

    Surface(
        color = backgroundColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Date cell with flexible weight
            TableCell(
                text = displayDate,
                weight = 1.3f,
                isTitle = true,
                fontSize = 14.sp
            )
            
            for (i in 0 until slotCount) {
                val data = summary.slots[i]
                val text = data?.let { 
                    val bp = "${it.systolic}/${it.diastolic}"
                    if (it.pulse > 0) {
                        if (slotCount < 3) "$bp@${it.pulse}" else "$bp\n@${it.pulse}"
                    } else {
                        bp
                    }
                } ?: stringResource(R.string.empty_value)

                val classification = data?.let { 
                    BloodPressureClassifier.classify(it.systolic, it.diastolic, guidelines)
                }
                
                TableCell(
                    text = text, 
                    weight = 1f,
                    fontSize = measurementFontSize,
                    isBold = classification?.isBold ?: false,
                    textColor = if (isSummaryVisible) (classification?.textColor ?: Color.Unspecified) else MaterialTheme.colorScheme.onSurfaceVariant,
                    backgroundColor = Color.Transparent,
                    trackerValues = data?.trackerValues ?: emptyMap(),
                    activeTrackers = activeTrackers,
                    onClick = if (summary.isToday && summary.clickableSlots.contains(i)) { { onCellClick(i) } } else null
                )
            }
        }
    }
}

@Composable
private fun RowScope.TableCell(
    text: String,
    weight: Float,
    fontSize: TextUnit = 12.sp,
    isTitle: Boolean = false,
    isBold: Boolean = false,
    textColor: Color = Color.Unspecified,
    backgroundColor: Color = Color.Transparent,
    trackerValues: Map<Long, TrackerValue> = emptyMap(),
    activeTrackers: List<TrackerDefinition> = emptyList(),
    onClick: (() -> Unit)? = null
) {
    Surface(
        color = backgroundColor,
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 2.dp)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp, horizontal = 4.dp),
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = textColor,
                    fontSize = fontSize,
                    fontWeight = if (isTitle || isBold) FontWeight.Bold else FontWeight.Normal,
                    lineHeight = fontSize * 1.2f
                ),
                textAlign = if (isTitle) TextAlign.Start else TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            TrackerIndicatorBadge(
                trackerValues = trackerValues,
                activeTrackers = activeTrackers,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 2.dp, end = 2.dp)
            )
        }
    }
}
