package com.otakeessen.underpressure.ui.table.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import com.otakeessen.underpressure.domain.TrackerDefinition
import com.otakeessen.underpressure.domain.TrackerType
import com.otakeessen.underpressure.domain.TrackerValue
import com.otakeessen.underpressure.domain.validation.BloodPressureValidator
import com.otakeessen.underpressure.domain.validation.ValidationResult
import com.otakeessen.underpressure.ui.table.MeasurementDialogState
import com.otakeessen.underpressure.ui.util.BpLevelMapper

@Composable
fun MeasurementEditDialog(
    state: MeasurementDialogState,
    guidelines: BpGuidelines,
    onValueChange: (TextFieldValue) -> Unit,
    onTrackerValueChange: (Long, TrackerValue) -> Unit,
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

    var isTrackersExpanded by remember(state.isOpen) {
        mutableStateOf(
            state.activeTrackers.any { tracker ->
                val value = state.trackerValues[tracker.id]
                value?.floatValue != null || (value?.booleanValue == true) || !value?.stringValue.isNullOrBlank()
            }
        )
    }

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

    val isAnyTrackerError = state.activeTrackers.any { tracker ->
        if (tracker.type == TrackerType.FLOAT) {
            val value = state.trackerValues[tracker.id]?.floatValue
            value != null && (
                (tracker.min != null && value < tracker.min) ||
                (tracker.max != null && value > tracker.max)
            )
        } else false
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // ... (rest of the UI implementation)
                if (state.isFlexibleMode) {
                    Text(
                        text = stringResource(R.string.label_anytime_reading) + " — ${state.slotTime}",
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

                Spacer(modifier = Modifier.height(8.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (classification != null) {
                        ClassificationStatusPill(
                            classification = classification,
                            text = bpLevelText ?: ""
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = textFieldValue,
                    onValueChange = onValueChange,
                    label = { Text(stringResource(R.string.label_measurement_format)) },
                    placeholder = { Text(stringResource(R.string.placeholder_measurement)) },
                    isError = isError,
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
                        imeAction = if (state.activeTrackers.isEmpty()) ImeAction.Done else ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (validationResult is ValidationResult.Success && !isAnyTrackerError) {
                                onSave(textValue)
                            }
                        }
                    ),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                if (isError) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp, start = 8.dp)
                    )
                }

                if (state.activeTrackers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    val rotationState by animateFloatAsState(
                        targetValue = if (isTrackersExpanded) 180f else 0f,
                        label = "rotation"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isTrackersExpanded = !isTrackersExpanded }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.additional_info),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            val activeCount = state.activeTrackers.count { tracker ->
                                val valObj = state.trackerValues[tracker.id]
                                valObj?.floatValue != null || (valObj?.booleanValue == true) || !valObj?.stringValue.isNullOrBlank()
                            }
                            Text(
                                text = if (activeCount > 0) {
                                    stringResource(R.string.active_custom_reading_s, activeCount)
                                } else {
                                    stringResource(R.string.tap_to_show_hide_custom_readings)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isTrackersExpanded) "Collapse" else "Expand",
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(rotationState),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    AnimatedVisibility(
                        visible = isTrackersExpanded,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column {
                            state.activeTrackers.forEach { tracker ->
                                val currentValue = state.trackerValues[tracker.id] ?: TrackerValue(
                                    measurementId = state.existingMeasurementId ?: 0,
                                    trackerId = tracker.id
                                )
                                
                                TrackerInput(
                                    tracker = tracker,
                                    value = currentValue,
                                    onValueChange = { onTrackerValueChange(tracker.id, it) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(textValue) },
                enabled = validationResult is ValidationResult.Success && !isAnyTrackerError
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
fun TrackerInput(
    tracker: TrackerDefinition,
    value: TrackerValue,
    onValueChange: (TrackerValue) -> Unit
) {
    when (tracker.type) {
        TrackerType.FLOAT -> {
            var rawInput by remember(value.floatValue) {
                mutableStateOf(value.floatValue?.toString() ?: "")
            }
            
            // Check for format validity: either empty or valid number
            val isFormatError = rawInput.isNotEmpty() && rawInput.replace(',', '.').toDoubleOrNull() == null
            val numericVal = rawInput.replace(',', '.').toDoubleOrNull()
            
            // Check for range validity: only if valid numeric value
            val isRangeError = numericVal != null && (
                (tracker.min != null && numericVal < tracker.min) ||
                (tracker.max != null && numericVal > tracker.max)
            )

            val isError = isFormatError || isRangeError

            OutlinedTextField(
                value = rawInput,
                onValueChange = { str ->
                    rawInput = str
                    val newVal = str.replace(',', '.').toDoubleOrNull()
                    onValueChange(value.copy(floatValue = newVal))
                },
                label = { 
                    val label = tracker.name + (tracker.unit?.let { " ($it)" } ?: "")
                    Text(label) 
                },
                isError = isError,
                supportingText = {
                    if (isFormatError) {
                        Text(stringResource(R.string.invalid_number_format))
                    } else if (isRangeError) {
                        Text(
                            stringResource(
                                R.string.must_be_between_and,
                                tracker.min ?: "-∞",
                                tracker.max ?: "∞"
                            ))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        TrackerType.BOOLEAN -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = value.booleanValue ?: false,
                    onCheckedChange = { onValueChange(value.copy(booleanValue = it)) }
                )
                Text(text = tracker.name)
            }
        }
        TrackerType.STRING -> {
            OutlinedTextField(
                value = value.stringValue ?: "",
                onValueChange = { onValueChange(value.copy(stringValue = it)) },
                label = { Text(tracker.name) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
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
