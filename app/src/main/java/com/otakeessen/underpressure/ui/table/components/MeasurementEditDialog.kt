package com.otakeessen.underpressure.ui.table.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.otakeessen.underpressure.domain.BloodPressureClassifier
import com.otakeessen.underpressure.domain.BloodPressureLevel
import com.otakeessen.underpressure.domain.BpGuidelines
import com.otakeessen.underpressure.domain.validation.BloodPressureValidator
import com.otakeessen.underpressure.domain.validation.ValidationResult
import com.otakeessen.underpressure.ui.table.MeasurementDialogState
import com.otakeessen.underpressure.ui.util.BpLevelMapper

/**
 * Dialog for entering or editing a blood pressure measurement.
 */
@Composable
fun MeasurementEditDialog(
    state: MeasurementDialogState,
    guidelines: BpGuidelines,
    onValueChange: (TextFieldValue) -> Unit,
    onSave: (String) -> Unit,
    onAcceptGuidance: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isOpen) return

    if (state.isGuidanceVisible) {
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
    
    // Trim for validation to allow saving when pulse is omitted but delimiter is present
    val trimmedForValidation = textValue.trim().removeSuffix("@").removeSuffix("/").trim()
    val validationResult = validator.validate(trimmedForValidation)
    
    val isError = textValue.isNotEmpty() && validationResult is ValidationResult.Error
    
    // Hypertension classification
    val classification = if (validationResult is ValidationResult.Success) {
        BloodPressureClassifier.classify(validationResult.systolic, validationResult.diastolic, guidelines)
    } else null
    
    val isHypertension = classification != null && classification.level >= BloodPressureLevel.STAGE_2

    val bpLevelText = classification?.let {
        stringResource(BpLevelMapper.getStringRes(it.level, guidelines))
    }

    val errorMessage = when (validationResult) {
        is ValidationResult.Error.IncorrectMeasurements, 
        is ValidationResult.Error.InvalidNumbers -> stringResource(R.string.error_incorrect_measurements)
        else -> stringResource(R.string.error_invalid_format)
    }
    
    val focusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current
    var lastLength by remember { mutableStateOf(textValue.length) }

    LaunchedEffect(state.isOpen) {
        if (state.isOpen) {
            focusRequester.requestFocus()
        }
    }

    // Trigger haptic feedback when a delimiter is added
    LaunchedEffect(textValue) {
        if (textValue.length > lastLength && (textValue.endsWith("/") || textValue.endsWith("@") || textValue.endsWith(" "))) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        lastLength = textValue.length
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
                    if (classification != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        if (isHypertension) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = classification.textColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = bpLevelText ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = classification.textColor
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
                    colors = if (classification != null && classification.level != BloodPressureLevel.NORMAL && !isError) {
                        OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = classification.textColor,
                            unfocusedBorderColor = classification.textColor.copy(alpha = 0.5f),
                            focusedLabelColor = classification.textColor,
                            cursorColor = classification.textColor
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
