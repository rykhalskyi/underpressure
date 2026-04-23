package com.otakeessen.underpressure.ui.table.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otakeessen.underpressure.R
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
    onCellClick: (slotIndex: Int) -> Unit,
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
                    // Highlighting BP and showing Pulse on next line if space allows
                    if (slotCount < 3) {
                        "${it.systolic}/${it.diastolic}@${it.pulse}"
                    } else {
                        "${it.systolic}/${it.diastolic}\n@${it.pulse}"
                    }
                } ?: stringResource(R.string.empty_value)
                
                TableCell(
                    text = text, 
                    weight = 1f,
                    fontSize = measurementFontSize,
                    isBold = false,//data != null,
                    onClick = if (summary.isToday) { { onCellClick(i) } } else null
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
    onClick: (() -> Unit)? = null
) {
    Text(
        text = text,
        modifier = Modifier
            .weight(weight)
            .padding(horizontal = 4.dp)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = fontSize,
            fontWeight = if (isTitle || isBold) FontWeight.Bold else FontWeight.Normal,
            lineHeight = fontSize * 1.2f
        ),
        textAlign = if (isTitle) TextAlign.Start else TextAlign.Center,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}

