package com.otakeessen.underpressure.ui.table.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Assignment
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otakeessen.underpressure.domain.TrackerDefinition
import com.otakeessen.underpressure.domain.TrackerType
import com.otakeessen.underpressure.domain.TrackerValue

/**
 * A beautiful badge/icon indicator that lists non-empty custom trackers in a balloon dropdown upon tapping.
 */
@Composable
fun TrackerIndicatorBadge(
    trackerValues: Map<Long, TrackerValue>,
    activeTrackers: List<TrackerDefinition>,
    modifier: Modifier = Modifier
) {
    val nonEmptyTrackers = remember(trackerValues, activeTrackers) {
        activeTrackers.mapNotNull { tracker ->
            val value = trackerValues[tracker.id]
            if (value != null) {
                val hasValue = when (tracker.type) {
                    TrackerType.FLOAT -> value.floatValue != null
                    TrackerType.BOOLEAN -> value.booleanValue == true
                    TrackerType.STRING -> !value.stringValue.isNullOrEmpty()
                }
                if (hasValue) tracker to value else null
            } else {
                null
            }
        }
    }

    if (nonEmptyTrackers.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .size(24.dp)
            .clickable { expanded = true },
        contentAlignment = Alignment.Center
    ) {
        // Highly illustrative assignment checklist icon
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Assignment,
            contentDescription = "Custom Trackers",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(13.dp)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(8.dp)
            ) {
                Text(
                    text = "Trackers",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                nonEmptyTrackers.forEach { (tracker, value) ->
                    val displayValue = when (tracker.type) {
                        TrackerType.FLOAT -> "${value.floatValue} ${tracker.unit ?: ""}"
                        TrackerType.BOOLEAN -> "Yes"
                        TrackerType.STRING -> value.stringValue ?: ""
                    }
                    Text(
                        text = "• ${tracker.name}: $displayValue",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}
