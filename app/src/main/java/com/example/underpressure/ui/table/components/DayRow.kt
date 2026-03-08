package com.example.underpressure.ui.table.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.underpressure.R
import com.example.underpressure.ui.table.DayMeasurementSummary

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
    val backgroundColor = if (summary.isToday) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Surface(
        color = backgroundColor,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 12.dp, horizontal = 16.dp)
                .fillMaxWidth()
        ) {
            TableCell(text = summary.date, weight = 1.5f, isTitle = true)
            
            for (i in 0 until slotCount) {
                val data = summary.slots[i]
                val text = data?.let { "${it.systolic}/${it.diastolic}@${it.pulse}" }
                    ?: stringResource(R.string.empty_value)
                TableCell(
                    text = text, 
                    weight = 1f,
                    onClick = { onCellClick(i) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.TableCell(
    text: String,
    weight: Float,
    isTitle: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    Text(
        text = text,
        modifier = Modifier
            .weight(weight)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        style = if (isTitle) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold) 
                else MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
        textAlign = TextAlign.Start,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
