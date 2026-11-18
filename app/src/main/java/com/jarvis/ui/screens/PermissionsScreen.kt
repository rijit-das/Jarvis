package com.jarvis.ui.screens

import android.Manifest
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.jarvis.core.permissions.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    val permissionManager: PermissionManager
) : ViewModel()

private data class PermissionRow(
    val permission: String,
    val title: String,
    val rationale: String
)

private val permissionRows = listOf(
    PermissionRow(
        Manifest.permission.RECORD_AUDIO,
        "Microphone",
        "Needed for voice input, wake word, and clap detection. Audio is processed on-device when possible."
    ),
    PermissionRow(
        Manifest.permission.POST_NOTIFICATIONS,
        "Notifications",
        "Needed to show reminders and the voice-session indicator."
    )
)

@Composable
fun PermissionsScreen(viewModel: PermissionsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var refreshTick by remember { mutableStateOf(0) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshTick++ }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Permissions dashboard", style = MaterialTheme.typography.headlineMedium)
        Text(
            "JARVIS requests each permission in context, with an explanation. " +
                "You can revoke any permission here or in system settings at any time.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        permissionRows.forEach { row ->
            // refreshTick is read to recompose after permission changes.
            @Suppress("UNUSED_EXPRESSION") refreshTick
            val granted = viewModel.permissionManager.isGranted(row.permission)
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(row.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (granted) "Granted" else "Not granted",
                            color = if (granted) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(row.rationale, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    if (!granted) {
                        Button(onClick = { launcher.launch(row.permission) }) {
                            Text("Request")
                        }
                    } else {
                        OutlinedButton(onClick = {
                            context.startActivity(viewModel.permissionManager.appSettingsIntent())
                        }) { Text("Open app settings") }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Special access", style = MaterialTheme.typography.titleMedium)
        Text(
            "Notification access: JARVIS cannot enable this itself. " +
                "Enable it manually in system Settings → Notifications → Notification access " +
                "if you want JARVIS to read notifications.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val listenerEnabled = viewModel.permissionManager.isNotificationListenerEnabled()
        Text(
            if (listenerEnabled) "Notification access: Enabled" else "Notification access: Disabled",
            color = if (listenerEnabled) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error
        )
        OutlinedButton(onClick = {
            context.startActivity(viewModel.permissionManager.notificationAccessIntent())
        }) { Text("Open notification-access settings") }
    }
}
