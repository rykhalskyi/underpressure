package com.example.underpressure.ui.table.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.underpressure.R

/**
 * Header row for the measurement table.
 *
 * @param modifier Modifier to be applied to the header.
 */
@Composable
fun TableHeader(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 8.dp, horizontal = 16.dp)
    ) {
        HeaderCell(text = stringResource(R.string.header_date), weight = 1.5f)
        HeaderCell(text = stringResource(R.string.header_systolic), weight = 1f)
        HeaderCell(text = stringResource(R.string.header_diastolic), weight = 1f)
        HeaderCell(text = stringResource(R.string.header_pulse), weight = 1f)
    }
}

/**
 * A simple header cell.
 */
@Composable
private fun RowScope.HeaderCell(
    text: String,
    weight: Float
) {
    Text(
        text = text,
        modifier = Modifier.weight(weight),
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Start
    )
}
