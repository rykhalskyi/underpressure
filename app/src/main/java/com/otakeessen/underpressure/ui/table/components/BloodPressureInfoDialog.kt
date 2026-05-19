package com.otakeessen.underpressure.ui.table.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.otakeessen.underpressure.R
import com.otakeessen.underpressure.domain.BpGuidelines
import com.otakeessen.underpressure.ui.util.BpLevelMapper

@Composable
fun BloodPressureInfoDialog(
    guidelines: BpGuidelines,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sourceRes = BpLevelMapper.getSourceStringRes(guidelines)
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    // AHA/ACC Crisis threshold is >180/120; ESC/ESH Grade 3 threshold is ≥180/110
    val crisisText = if (guidelines == BpGuidelines.AHA_ACC) {
        stringResource(R.string.severe_blood_pressure_elevation_180_120)
    } else {
        stringResource(R.string.severe_blood_pressure_elevation_180_110)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = modifier.widthIn(max = screenWidth * 0.9f),
        title = {
            Text(
                text = stringResource(R.string.dialog_title_bp_info),
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                InfoSection(
                    title = stringResource(R.string.bp_info_what_is_title),
                    description = stringResource(R.string.bp_info_what_is_desc)
                )

                SubSection(
                    title = stringResource(R.string.bp_info_systolic_title),
                    description = stringResource(R.string.bp_info_systolic_desc)
                )

                SubSection(
                    title = stringResource(R.string.bp_info_diastolic_title),
                    description = stringResource(R.string.bp_info_diastolic_desc)
                )

                Spacer(modifier = Modifier.height(16.dp))

                InfoSection(
                    title = stringResource(R.string.bp_info_why_matters_title),
                    description = stringResource(R.string.bp_info_why_matters_desc)
                )

                Spacer(modifier = Modifier.height(16.dp))

                InfoSection(
                    title = stringResource(R.string.bp_info_silent_killer_title),
                    description = stringResource(R.string.bp_info_silent_killer_desc)
                )

                Spacer(modifier = Modifier.height(16.dp))

                InfoSection(
                    title = crisisText,
                    description = stringResource(R.string.if_your_blood_pressure_is_consistently_this_high_or_higher)
                )

                SubSection(
                    title = stringResource(R.string._1_pause_and_re_test),
                    description = stringResource(R.string.wait_5_minutes_sit_quietly_and_re_test_if_it_remains_high_consult_your_doctor)
                )

                SubSection(
                    title = stringResource(R.string._2_check_for_emergency_symptoms),
                    description = stringResource(R.string.if_you_have_high_bp_and_symptoms_like_chest_pain_shortness_of_breath_numbness_confusion_or_severe_headache_seek_emergency_medical_care_immediately),
                    isEmergency = true
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(sourceRes),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_close))
            }
        }
    )
}

@Composable
private fun InfoSection(
    title: String,
    description: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SubSection(
    title: String,
    description: String,
    isEmergency: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (isEmergency) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
