package com.otakeessen.underpressure.ui.table.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

    val total = stats.values.sum().toFloat()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "${stringResource(R.string.header_classification_summary)} (${stringResource(R.string.label_classification_all_time)})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Bar visualization
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
        ) {
            BloodPressureLevel.values().forEach { level ->
                val count = stats[level] ?: 0
                if (count > 0) {
                    val weight = count / total
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(weight)
                            .height(24.dp)
                            .background(level.toColor())
                    )
                }
            }
        }

        // Legend with percentages
        Column(modifier = Modifier.padding(top = 8.dp)) {
            BloodPressureLevel.values().forEach { level ->
                val count = stats[level] ?: 0
                if (count > 0) {
                    val percentage = (count / total * 100).toInt()
                    val labelRes = when (level) {
                        BloodPressureLevel.NORMAL -> R.string.bp_level_normal
                        BloodPressureLevel.ELEVATED -> if (guidelines == BpGuidelines.ESC_ESH) R.string.bp_level_high_normal else R.string.bp_level_elevated
                        BloodPressureLevel.STAGE_1 -> if (guidelines == BpGuidelines.ESC_ESH) R.string.bp_level_grade1 else R.string.bp_level_stage1
                        BloodPressureLevel.STAGE_2 -> if (guidelines == BpGuidelines.ESC_ESH) R.string.bp_level_grade2 else R.string.bp_level_stage2
                        BloodPressureLevel.CRISIS -> R.string.bp_level_crisis
                    }
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(12.dp) // use size for square
                                .background(level.toColor(), RoundedCornerShape(2.dp))
                        )
                        Text(
                            text = "${stringResource(labelRes)}: $percentage%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
