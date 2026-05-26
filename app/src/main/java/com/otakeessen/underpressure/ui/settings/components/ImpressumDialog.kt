package com.otakeessen.underpressure.ui.settings.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.otakeessen.underpressure.R

@Composable
fun ImpressumDialog(
    content: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val email = "otakeessen@gmail.com"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.label_impressum)) },
        text = {
            Column {
                Text(
                    text = content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$email")
                    }
                    context.startActivity(Intent.createChooser(intent, "Send email"))
                }
            ) {
                Text(stringResource(R.string.contact_developer))
            }
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.button_ok))
            }
        }
    )
}
