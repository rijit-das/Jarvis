package com.jarvis.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Confirmation gate for consequential actions. The description is shown
 * verbatim so the user knows exactly what they're approving.
 */
@Composable
fun ConfirmationDialog(
    description: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm action") },
        text = { Text(description) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Allow") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
