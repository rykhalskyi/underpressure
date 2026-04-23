package com.otakeessen.underpressure.ui.table.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.otakeessen.underpressure.R
import com.otakeessen.underpressure.domain.validation.BloodPressureValidator
import com.otakeessen.underpressure.domain.validation.ValidationResult
import com.otakeessen.underpressure.ui.table.MeasurementDialogState

/**
 * Dialog for entering or editing a blood pressure measurement.
 */
@Composable
fun MeasurementEditDialog(
    state: MeasurementDialogState,
    onSave: (String) -> Unit,
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

