package com.jarvis.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository
) : ViewModel() {
    val voiceOutput = settings.voiceOutputEnabled
    val ttsRate = settings.ttsRate
    val ttsPitch = settings.ttsPitch
    val wakeWord = settings.wakeWordEnabled
    val clap = settings.clapEnabled
    val clapThreshold = settings.clapThreshold
    val localeTag = settings.localeTag

    fun setVoiceOutput(value: Boolean) = viewModelScope.launch { settings.setVoiceOutputEnabled(value) }
    fun setTtsRate(value: Float) = viewModelScope.launch { settings.setTtsRate(value) }
    fun setTtsPitch(value: Float) = viewModelScope.launch { settings.setTtsPitch(value) }
    fun setWakeWord(value: Boolean) = viewModelScope.launch { settings.setWakeWordEnabled(value) }
    fun setClap(value: Boolean) = viewModelScope.launch { settings.setClapEnabled(value) }
    fun setClapThreshold(value: Float) = viewModelScope.launch { settings.setClapThreshold(value) }
    fun setLocaleTag(value: String) = viewModelScope.launch { settings.setLocaleTag(value) }
}

@Composable
fun SettingsScreen(
    onOpenModelSettings: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenActivity: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val voiceOutput by viewModel.voiceOutput.collectAsState(initial = true)
    val rate by viewModel.ttsRate.collectAsState(initial = 1.0f)
    val pitch by viewModel.ttsPitch.collectAsState(initial = 1.0f)
    val wakeWord by viewModel.wakeWord.collectAsState(initial = false)
    val clap by viewModel.clap.collectAsState(initial = false)
    val clapThreshold by viewModel.clapThreshold.collectAsState(initial = 0.35f)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        Text("Voice", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Voice output", modifier = Modifier.weight(1f))
            Switch(checked = voiceOutput, onCheckedChange = { viewModel.setVoiceOutput(it) })
        }
        Text("Speech rate: ${"%.1f".format(rate)}x")
        Slider(value = rate, onValueChange = { viewModel.setTtsRate(it) }, valueRange = 0.5f..2.0f)
        Text("Pitch: ${"%.1f".format(pitch)}")
        Slider(value = pitch, onValueChange = { viewModel.setTtsPitch(it) }, valueRange = 0.5f..2.0f)
        Spacer(Modifier.height(16.dp))

        Text("Activation", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Wake word (\"Hey JARVIS\")", modifier = Modifier.weight(1f))
            Switch(checked = wakeWord, onCheckedChange = { viewModel.setWakeWord(it) })
        }
        Text(
            "Best-effort, foreground-bound, and battery-costly. See Known Limitations.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Clap activation (double clap)", modifier = Modifier.weight(1f))
            Switch(checked = clap, onCheckedChange = { viewModel.setClap(it) })
        }
        Text("Clap sensitivity threshold: ${"%.2f".format(clapThreshold)}")
        Slider(
            value = clapThreshold,
            onValueChange = { viewModel.setClapThreshold(it) },
            valueRange = 0.1f..0.9f
        )
        Spacer(Modifier.height(16.dp))

        Text("Sections", style = MaterialTheme.typography.titleMedium)
        SettingsRow("AI model settings", onOpenModelSettings)
        SettingsRow("Privacy & security center", onOpenPrivacy)
        SettingsRow("Permissions dashboard", onOpenPermissions)
        SettingsRow("Activity history", onOpenActivity)
        SettingsRow("Diagnostics", onOpenDiagnostics)
    }
}

@Composable
private fun SettingsRow(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null)
    }
}
