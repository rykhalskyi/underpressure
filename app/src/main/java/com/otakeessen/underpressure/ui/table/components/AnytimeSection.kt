package com.otakeessen.underpressure.ui.table.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otakeessen.underpressure.R
import com.otakeessen.underpressure.domain.BloodPressureClassifier
import com.otakeessen.underpressure.domain.BpGuidelines
import com.otakeessen.underpressure.domain.TrackerDefinition
import com.otakeessen.underpressure.domain.TrackerType
import com.otakeessen.underpressure.ui.table.AnytimeReadingData
import com.otakeessen.underpressure.ui.util.BpLevelMapper
import java.time.LocalDate

@Composable
fun AnytimeSection(
    date: String,
    readings: List<AnytimeReadingData>,
    guidelines: BpGuidelines,
    activeTrackers: List<TrackerDefinition> = emptyList(),
    isSummaryVisible: Boolean,
    onReadingClick: (String, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val todayStr = LocalDate.now().toString()
    val isToday = date == todayStr

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)
    ) {
        Text(
            text = stringResource(R.string.label_anytime_readings),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        readings.forEach { reading ->
            val classification = BloodPressureClassifier.classify(
                reading.systolic, reading.diastolic, guidelines
            )
            val bpText = buildString {
                append("${reading.systolic}/${reading.diastolic}")
                if (reading.pulse > 0) append(" @${reading.pulse}")
            }
            
            val trackerIndicator = activeTrackers.filter { t -> reading.trackerValues.containsKey(t.id) }
                .joinToString("") { t -> 
                    when {
                        t.name.contains("Weight", ignoreCase = true) -> "⚖️"
                        t.name.contains("Temp", ignoreCase = true) -> "🌡️"
                        t.name.contains("Med", ignoreCase = true) || t.name.contains("Pill", ignoreCase = true) -> "💊"
                        t.type == TrackerType.BOOLEAN -> "✅"
                        else -> "📝"
                    }
                }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isToday) Modifier.clickable { onReadingClick(date, reading.id) }
                        else Modifier
                    )
                    .padding(vertical = 2.dp)
            ) {
                Text(
                    text = reading.timeStr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(48.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = bpText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSummaryVisible) (classification?.textColor
                        ?: MaterialTheme.colorScheme.onSurface)
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (classification?.isBold == true) FontWeight.Bold else FontWeight.Normal
                )
                
                if (trackerIndicator.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = trackerIndicator, fontSize = 10.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = classification?.let {
                        stringResource(BpLevelMapper.getStringRes(it.level, guidelines))
                    } ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isSummaryVisible) (classification?.textColor
                        ?: MaterialTheme.colorScheme.onSurfaceVariant)
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
