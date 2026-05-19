package com.otakeessen.underpressure.ui.table.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otakeessen.underpressure.R
import com.otakeessen.underpressure.domain.BloodPressureLevel
import com.otakeessen.underpressure.domain.BpGuidelines
import com.otakeessen.underpressure.domain.BloodPressureClassifier.toColor
import com.otakeessen.underpressure.ui.util.BpLevelMapper

/**
 * Displays summary statistics for blood pressure classifications.
 */
@Composable
fun ClassificationSummary(
    stats: Map<BloodPressureLevel, Int>,
    guidelines: BpGuidelines,
    modifier: Modifier = Modifier
) {
    if (stats.isEmpty()) return

    var showInfoDialog by remember { mutableStateOf(false) }

    if (showInfoDialog) {
        BloodPressureInfoDialog(
            guidelines = guidelines,
            onDismiss = { showInfoDialog = false }
        )
    }

    val total = stats.values.sum().toFloat()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Title row with info icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${stringResource(R.string.header_classification_summary)} (${stringResource(R.string.label_classification_all_time)})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            IconButton(
                onClick = { showInfoDialog = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = stringResource(R.string.cd_bp_info),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Bar visualization
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(6.dp))
        ) {
            BloodPressureLevel.entries.forEach { level ->
                val count = stats[level] ?: 0
                if (count > 0) {
                    val weight = count / total
                    Box(
                        modifier = Modifier
                            .weight(weight)
                            .height(24.dp)
                            .background(level.toColor())
                    )
                }
            }
        }

        // Legend with percentages and ranges
        Column(modifier = Modifier.padding(top = 12.dp)) {
            BloodPressureLevel.entries.forEach { level ->
                val count = stats[level] ?: 0
                if (count > 0) {
                    val percentage = (count / total * 100).toInt()
                    val labelRes = BpLevelMapper.getStringRes(level, guidelines)
                    val rangeRes = BpLevelMapper.getRangeStringRes(level, guidelines)
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(12.dp)
                                .background(level.toColor(), RoundedCornerShape(2.dp))
                        )
                        
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(labelRes),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = " (${stringResource(rangeRes)})",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "${percentage}%",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Source Attribution
        Text(
            text = stringResource(BpLevelMapper.getSourceStringRes(guidelines)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}
