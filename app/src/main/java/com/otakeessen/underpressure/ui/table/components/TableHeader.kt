package com.otakeessen.underpressure.ui.table.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.otakeessen.underpressure.R

/**
 * Header row for the measurement table with dynamic slots.
 */
@Composable
fun TableHeader(
    slotHeaders: List<String>,
    onSlotClick: (Int) -> Unit,
    activeSlotIndex: Int? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 8.dp, horizontal = 12.dp)
    ) {
        HeaderCell(
            text = stringResource(R.string.header_date),
            weight = 1.3f,
            textAlign = TextAlign.Start
        )
        
        slotHeaders.forEachIndexed { index, time ->
            val isActive = index == activeSlotIndex
            HeaderCell(
                text = time,
                weight = 1f,
                textAlign = TextAlign.Center,
                onClick = { onSlotClick(index) },
                isActive = isActive
            )
        }
    }
}

@Composable
private fun RowScope.HeaderCell(
    text: String,
    weight: Float,
    textAlign: TextAlign,
    onClick: (() -> Unit)? = null,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier
            .weight(weight)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            textDecoration = if (isActive) androidx.compose.ui.text.style.TextDecoration.Underline else null
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = textAlign,
        maxLines = 1
    )
}

