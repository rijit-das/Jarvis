package com.jarvis.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.data.datastore.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrivacyViewModel @Inject constructor(
    private val settings: SettingsRepository
) : ViewModel() {
    val offlineMode = settings.offlineMode
    val memoryEnabled = settings.memoryEnabled
    val auditEnabled = settings.auditEnabled
    val voiceOutput = settings.voiceOutputEnabled

    fun setOffline(value: Boolean) = viewModelScope.launch { settings.setOfflineMode(value) }
    fun setMemory(value: Boolean) = viewModelScope.launch { settings.setMemoryEnabled(value) }
    fun setAudit(value: Boolean) = viewModelScope.launch { settings.setAuditEnabled(value) }
    fun setVoiceOutput(value: Boolean) = viewModelScope.launch { settings.setVoiceOutputEnabled(value) }
}

@Composable
fun PrivacyScreen(viewModel: PrivacyViewModel = hiltViewModel()) {
    val offline by viewModel.offlineMode.collectAsState(initial = true)
    val memory by viewModel.memoryEnabled.collectAsState(initial = true)
    val audit by viewModel.auditEnabled.collectAsState(initial = true)
    val voice by viewModel.voiceOutput.collectAsState(initial = true)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("Privacy & security center", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        Text("Offline mode", style = MaterialTheme.typography.titleMedium)
        Text(
            "When ON, JARVIS makes no network calls. This is the default and recommended setting.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Switch(checked = offline, onCheckedChange = { viewModel.setOffline(it) })
        Spacer(Modifier.height(16.dp))

        Text("Memory", style = MaterialTheme.typography.titleMedium)
        Text(
            "When ON, JARVIS may store facts you explicitly ask it to remember. " +
                "You can inspect, edit, export, and delete them in the Memory screen. " +
                "Sensitive information is never stored by default, and raw microphone recordings are never stored.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Switch(checked = memory, onCheckedChange = { viewModel.setMemory(it) })
        Spacer(Modifier.height(16.dp))

        Text("Activity log", style = MaterialTheme.typography.titleMedium)
        Text(
            "Records tool executions and security events (metadata only, never secrets) " +
                "so you can audit what JARVIS did.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Switch(checked = audit, onCheckedChange = { viewModel.setAudit(it) })
        Spacer(Modifier.height(16.dp))

        Text("Voice output", style = MaterialTheme.typography.titleMedium)
        Text(
            "When OFF, JARVIS will not speak responses. Text responses are always shown.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Switch(checked = voice, onCheckedChange = { viewModel.setVoiceOutput(it) })
        Spacer(Modifier.height(24.dp))

        Text("Data handling summary", style = MaterialTheme.typography.titleMedium)
        Text(
            "• All data is stored encrypted on this device (SQLCipher + Android Keystore).\n" +
            "• No audio is uploaded without explicit consent.\n" +
            "• Online mode is opt-in, consent-gated, and always indicated.\n" +
            "• Third-party speech/TTS engines are outside JARVIS's control; " +
            "their privacy implications depend on the installed engine.\n" +
            "• Personal data is excluded from cloud backups.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
