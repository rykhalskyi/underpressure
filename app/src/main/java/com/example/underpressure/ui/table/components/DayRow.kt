package com.example.underpressure.ui.table.components

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
import androidx.compose.ui.unit.dp
import com.example.underpressure.R
import com.example.underpressure.ui.table.DayMeasurementSummary

/**
 * A single row in the measurement table.
 *
 * @param summary The daily measurement data to display.
 * @param modifier Modifier to be applied to the row.
 */
@Composable
fun DayRow(
    summary: DayMeasurementSummary,
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
            TableCell(text = summary.systolic?.toString() ?: stringResource(R.string.empty_value), weight = 1f)
            TableCell(text = summary.diastolic?.toString() ?: stringResource(R.string.empty_value), weight = 1f)
            TableCell(text = summary.pulse?.toString() ?: stringResource(R.string.empty_value), weight = 1f)
        }
    }
}

/**
 * A simple cell in a row.
 */
@Composable
private fun RowScope.TableCell(
    text: String,
    weight: Float,
    isTitle: Boolean = false
) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        style = if (isTitle) MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold) 
                else MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Start
    )
}
