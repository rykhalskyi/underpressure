package com.otakeessen.underpressure.ui.table.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.otakeessen.underpressure.R
import com.otakeessen.underpressure.domain.validation.BloodPressureValidator
import com.otakeessen.underpressure.domain.validation.ValidationResult
import com.otakeessen.underpressure.ui.table.MeasurementDialogState
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * Dialog for entering or editing a blood pressure measurement.
 */
@Composable
fun MeasurementEditDialog(
    state: MeasurementDialogState,
    onValueChange: (TextFieldValue) -> Unit,
    onSave: (String) -> Unit,
    onAcceptGuidance: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isOpen) return

    if (state.isGuidanceVisible) {
        // ... (guidance dialog code remains same)
        AlertDialog(
            modifier = modifier,
            onDismissRequest = onDismiss,
            title = {
                Text(text = stringResource(R.string.guidance_title))
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.guidance_message,
                        state.slotIndex + 1,
                        state.suggestedSlotTime
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = onAcceptGuidance) {
                    Text(stringResource(R.string.button_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.button_cancel))
                }
            }
        )
        return
    }

    val textFieldValue = state.inputValue
    val textValue = textFieldValue.text
    val validator = remember { BloodPressureValidator() }
    val validationResult = validator.validate(textValue)
    val isError = textValue.isNotEmpty() && validationResult is ValidationResult.Error
    
    // Hypertension check (SYS >= 140 or DIA >= 90)
    val isHypertension = validationResult is ValidationResult.Success && 
            (validationResult.systolic >= 140 || validationResult.diastolic >= 90)

    val errorMessage = when (validationResult) {
        is ValidationResult.Error.IncorrectMeasurements, 
        is ValidationResult.Error.InvalidNumbers -> stringResource(R.string.error_incorrect_measurements)
        else -> stringResource(R.string.error_invalid_format)
    }
    
    val focusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state.isOpen) {
        if (state.isOpen) {
            focusRequester.requestFocus()
        }
    }

    // Trigger haptic feedback when a delimiter is added
    LaunchedEffect(textValue) {
        if (textValue.contains("/") || textValue.contains("@")) {
            // Only trigger if it's likely a new delimiter (simple heuristic)
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (state.existingMeasurementId == null) 
                    stringResource(R.string.dialog_title_add) 
                else stringResource(R.string.dialog_title_edit)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.dialog_measurement_slot_info, state.date, state.slotIndex + 1),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Format Legend
                Row(
                    modifier = Modifier.padding(bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SYS / DIA @ PULSE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                    if (isHypertension) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFF44336), // Red
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.hypertension_warning),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF44336)
                        )
                    }
                }
                
                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = onValueChange,
                    label = { Text(stringResource(R.string.label_measurement_format)) },
                    placeholder = { Text(stringResource(R.string.placeholder_measurement)) },
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text(text = errorMessage)
                        }
                    },
                    trailingIcon = {
                        if (textValue.isNotEmpty()) {
                            IconButton(onClick = { onValueChange(TextFieldValue("")) }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear input"
                                )
                            }
                        }
                    },
                    colors = if (isHypertension && !isError) {
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFF44336),
                            unfocusedBorderColor = Color(0xFFF44336).copy(alpha = 0.5f),
                            focusedLabelColor = Color(0xFFF44336),
                            cursorColor = Color(0xFFF44336)
                        )
                    } else OutlinedTextFieldDefaults.colors(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (validationResult is ValidationResult.Success) {
                                onSave(textValue)
                            }
                        }
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(textValue) },
                enabled = validationResult is ValidationResult.Success
            ) {
                Text(stringResource(R.string.button_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_cancel))
            }
        }
    )
}
