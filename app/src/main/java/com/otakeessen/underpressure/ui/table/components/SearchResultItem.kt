package com.otakeessen.underpressure.ui.table.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.otakeessen.underpressure.R
import com.otakeessen.underpressure.data.local.entities.MeasurementEntity
import com.otakeessen.underpressure.domain.BloodPressureClassifier
import com.otakeessen.underpressure.domain.BpGuidelines

/**
 * A reusable component to display an individual search result.
 */
@Composable
fun SearchResultItem(
    measurement: MeasurementEntity,
    guidelines: BpGuidelines,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = measurement.date,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            val details = if (measurement.pulse > 0) {
                stringResource(
                    R.string.label_measurement_details, 
                    measurement.systolic, 
                    measurement.diastolic, 
                    measurement.pulse
                )
            } else {
                val sysLabel = stringResource(R.string.header_systolic)
                val diaLabel = stringResource(R.string.header_diastolic)
                "$sysLabel: ${measurement.systolic}, $diaLabel: ${measurement.diastolic}"
            }
            
            val classification = BloodPressureClassifier.classify(
                measurement.systolic, 
                measurement.diastolic,
                guidelines
            )
            
            Text(
                text = details,
                style = MaterialTheme.typography.bodySmall,
                color = classification.textColor
            )
        }
    }
}
