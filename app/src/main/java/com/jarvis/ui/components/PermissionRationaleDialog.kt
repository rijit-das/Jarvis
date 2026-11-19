package com.jarvis.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Explains WHY a permission is needed before the system prompt appears.
 * JARVIS never requests permissions silently.
 */
@Composable
fun PermissionRationaleDialog(
    permissionName: String,
    rationale: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Permission: $permissionName") },
        text = { Text(rationale) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Continue") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        }
    )
}
