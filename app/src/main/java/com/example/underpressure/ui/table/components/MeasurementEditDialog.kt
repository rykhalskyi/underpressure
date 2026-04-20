package com.example.underpressure.ui.table.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.underpressure.R
import com.example.underpressure.domain.validation.BloodPressureValidator
import com.example.underpressure.domain.validation.ValidationResult
import com.example.underpressure.ui.table.MeasurementDialogState

/**
 * Dialog for entering or editing a blood pressure measurement.
 */
@Composable
fun MeasurementEditDialog(
    state: MeasurementDialogState,
    onSave: (String) -> Unit,
    onGenericInputChanged: (Long, String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isOpen) return

    var textValue by remember(state.initialValue) { mutableStateOf(state.initialValue) }
    val validator = remember { BloodPressureValidator() }
    val validationResult = validator.validate(textValue)
    val isError = textValue.isNotEmpty() && validationResult is ValidationResult.Error
    
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(state.isOpen) {
        if (state.isOpen) {
            focusRequester.requestFocus()
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
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text(stringResource(R.string.label_measurement_format)) },
                    placeholder = { Text(stringResource(R.string.placeholder_measurement)) },
                    isError = isError,
                    supportingText = {
                        if (isError) {
                            Text(text = stringResource(R.string.error_invalid_format))
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email // Provides '/' and '@' usually, or just use Phone
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                if (state.genericInputs.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Generic Measurements",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    state.genericInputs.forEach { input ->
                        when (input.type) {
                            "BOOLEAN" -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = input.value == "true",
                                        onCheckedChange = { onGenericInputChanged(input.listId, it.toString()) }
                                    )
                                    Text(input.name)
                                }
                            }
                            else -> {
                                OutlinedTextField(
                                    value = input.value,
                                    onValueChange = { onGenericInputChanged(input.listId, it) },
                                    label = { Text(input.name) },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = if (input.type == "DOUBLE") KeyboardType.Number else KeyboardType.Text
                                    )
                                )
                            }
                        }
                    }
                }
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
