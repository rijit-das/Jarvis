package com.jarvis.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.ai.model.CapabilityTier
import com.jarvis.ai.model.LocalLlmStatus
import com.jarvis.ai.model.ModelManager
import com.jarvis.data.datastore.SecureSettingsRepository
import com.jarvis.data.datastore.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelSettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val secureSettings: SecureSettingsRepository,
    private val modelManager: ModelManager
) : ViewModel() {

    private val _deviceSummary = MutableStateFlow("")
    val deviceSummary: StateFlow<String> = _deviceSummary.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val offlineMode = settings.offlineMode
    val onlineEnabled = settings.onlineModeEnabled
    val consentGiven = settings.onlineConsentGiven
    val baseUrl = settings.onlineBaseUrl
    val modelName = settings.onlineModel

    init {
        viewModelScope.launch {
            val report = modelManager.evaluate()
            val tierText = when (report.tier) {
                CapabilityTier.LOW -> "Low (intent engine only)"
                CapabilityTier.MEDIUM -> "Medium (full offline features)"
                CapabilityTier.HIGH -> "High (eligible for future on-device LLM)"
            }
            val llmText = when (val status = report.llmStatus) {
                is LocalLlmStatus.NotInstalled -> "No on-device model installed (v1 uses the intent engine)."
                is LocalLlmStatus.Ready -> "Model ready at ${status.modelPath}."
                is LocalLlmStatus.Incompatible -> "On-device LLM not supported: ${status.reason}"
            }
            _deviceSummary.value = "Capability tier: $tierText\n$llmText"
        }
    }

    fun setOfflineMode(value: Boolean) = viewModelScope.launch { settings.setOfflineMode(value) }
    fun setOnlineEnabled(value: Boolean) = viewModelScope.launch { settings.setOnlineModeEnabled(value) }
    fun setConsent(value: Boolean) = viewModelScope.launch { settings.setOnlineConsentGiven(value) }
    fun setBaseUrl(value: String) = viewModelScope.launch { settings.setOnlineBaseUrl(value) }
    fun setModelName(value: String) = viewModelScope.launch { settings.setOnlineModel(value) }

    fun saveApiKey(key: String) {
        secureSettings.setApiKey(key)
        _message.value = "API key saved (encrypted)."
    }

    fun clearApiKey() {
        secureSettings.clearApiKey()
        _message.value = "API key removed."
    }
}

@Composable
fun ModelSettingsScreen(viewModel: ModelSettingsViewModel = hiltViewModel()) {
    val offline by viewModel.offlineMode.collectAsState(initial = true)
    val onlineEnabled by viewModel.onlineEnabled.collectAsState(initial = false)
    val consent by viewModel.consentGiven.collectAsState(initial = false)
    val baseUrl by viewModel.baseUrl.collectAsState(initial = "https://api.openai.com/v1")
    val modelName by viewModel.modelName.collectAsState(initial = "gpt-4o-mini")
    val summary by viewModel.deviceSummary.collectAsState()
    val message by viewModel.message.collectAsState()
    var apiKeyInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("AI model settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(summary, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(24.dp))

        Text("Offline mode", style = MaterialTheme.typography.titleMedium)
        Text(
            "When ON (default), JARVIS never uses the network. All features run on-device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Switch(checked = offline, onCheckedChange = { viewModel.setOfflineMode(it) })
        Spacer(Modifier.height(16.dp))

        Text("Online mode (optional)", style = MaterialTheme.typography.titleMedium)
        Text(
            "Enables an OpenAI-compatible provider for open-ended conversation. " +
                "Your messages are sent to the provider over TLS. Only enable this if you trust the provider.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Switch(checked = onlineEnabled, onCheckedChange = { viewModel.setOnlineEnabled(it) })
        Spacer(Modifier.height(8.dp))
        Text("I understand my messages may be sent to the online provider.")
        Switch(checked = consent, onCheckedChange = { viewModel.setConsent(it) })
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = baseUrl,
            onValueChange = { viewModel.setBaseUrl(it) },
            label = { Text("Provider base URL") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = modelName,
            onValueChange = { viewModel.setModelName(it) },
            label = { Text("Model name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = apiKeyInput,
            onValueChange = { apiKeyInput = it },
            label = { Text("API key (stored encrypted)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            viewModel.saveApiKey(apiKeyInput)
            apiKeyInput = ""
        }) { Text("Save API key") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = { viewModel.clearApiKey() }) { Text("Remove API key") }
        message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.primary)
        }
    }
}
