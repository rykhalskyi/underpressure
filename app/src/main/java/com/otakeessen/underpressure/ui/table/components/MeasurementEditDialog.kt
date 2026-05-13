package com.otakeessen.underpressure.ui.table.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import com.otakeessen.underpressure.domain.ClassificationResult
import com.otakeessen.underpressure.domain.validation.BloodPressureValidator
import com.otakeessen.underpressure.domain.validation.ValidationResult
import com.otakeessen.underpressure.ui.table.MeasurementDialogState
import com.otakeessen.underpressure.ui.util.BpLevelMapper

@Composable
fun MeasurementEditDialog(
    state: MeasurementDialogState,
    guidelines: BpGuidelines,
    onValueChange: (TextFieldValue) -> Unit,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isOpen) return

    val textFieldValue = state.inputValue
    val textValue = textFieldValue.text
    val validator = remember { BloodPressureValidator() }

    val trimmedForValidation = textValue.trim().removeSuffix("@").removeSuffix("/").trim()
    val validationResult = validator.validate(trimmedForValidation)

    val isError = textValue.isNotEmpty() && validationResult is ValidationResult.Error

    val classification = if (validationResult is ValidationResult.Success) {
        BloodPressureClassifier.classify(validationResult.systolic, validationResult.diastolic, guidelines)
    } else null

    val bpLevelText = classification?.let {
        stringResource(BpLevelMapper.getStringRes(it.level, guidelines))
    }

    val errorMessage = when (validationResult) {
        is ValidationResult.Error.EmptyInput -> ""
        is ValidationResult.Error.InvalidFormat -> stringResource(R.string.error_syntax_format)
        is ValidationResult.Error.LogicalError -> stringResource(R.string.error_logic_sys_dia)
        is ValidationResult.Error.RangeError -> stringResource(R.string.error_range_out_of_human)
        else -> ""
    }

    val focusRequester = remember { FocusRequester() }
    val haptic = LocalHapticFeedback.current
    var lastLength by remember { mutableStateOf(textValue.length) }

    LaunchedEffect(state.isOpen) {
        if (state.isOpen) {
            focusRequester.requestFocus()
        }
    }

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
                if (state.isFlexibleMode) {
                    val nowFormatted = java.time.LocalTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                    Text(
                        text = stringResource(R.string.label_anytime_reading) + " — $nowFormatted",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = stringResource(
                            R.string.dialog_measurement_slot_info,
                            state.slotIndex + 1,
                            state.slotTime
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.date,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SYS / DIA @ PULSE",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Bold
                    )
                }

                if (classification != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ClassificationStatusPill(
                        classification = classification,
                        text = bpLevelText ?: ""
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

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

@Composable
private fun ClassificationStatusPill(
    classification: ClassificationResult,
    text: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = classification.backgroundColor
    ) {
        Text(
            text = text,
            color = classification.textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}
