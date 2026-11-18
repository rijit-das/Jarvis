package com.jarvis.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jarvis.ai.orchestrator.AssistantOrchestrator
import com.jarvis.data.datastore.SettingsRepository
import com.jarvis.ui.components.AudioVisualizer
import com.jarvis.ui.components.PermissionRationaleDialog
import com.jarvis.voice.ClapDetector
import com.jarvis.voice.SpeechRecognizerManager
import com.jarvis.voice.TextToSpeechManager
import com.jarvis.voice.VoiceSessionManager
import com.jarvis.voice.VoiceSessionState
import com.jarvis.voice.WakeWordDetector
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val voiceSessionManager: VoiceSessionManager,
    private val speechRecognizerManager: SpeechRecognizerManager,
    private val textToSpeechManager: TextToSpeechManager,
    private val orchestrator: AssistantOrchestrator,
    private val settings: SettingsRepository,
    private val wakeWordDetector: WakeWordDetector
) : ViewModel() {

    val sessionState: StateFlow<VoiceSessionState> = voiceSessionManager.state
    val partial = speechRecognizerManager.partial

    private val _lastTranscript = MutableStateFlow("")
    val lastTranscript: StateFlow<String> = _lastTranscript.asStateFlow()

    private val _lastResponse = MutableStateFlow("")
    val lastResponse: StateFlow<String> = _lastResponse.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var clapDetector: ClapDetector? = null

    init {
        voiceSessionManager.onTranscript = { transcript ->
            _lastTranscript.value = transcript
            viewModelScope.launch {
                val response = orchestrator.handleUserText(transcript)
                _lastResponse.value = response.spoken
                if (settings.voiceOutputEnabled.first()) {
                    voiceSessionManager.speak(response.spoken)
                }
            }
        }
        // Keep TTS config in sync with Settings.
        viewModelScope.launch {
            combine(
                settings.ttsRate, settings.ttsPitch, settings.voiceOutputEnabled
            ) { rate, pitch, enabled -> Triple(rate, pitch, enabled) }
                .collect { (rate, pitch, enabled) ->
                    textToSpeechManager.configure(rate, pitch, enabled)
                }
        }
        // Wake word: arm/disarm from Settings. On trigger, start a session.
        wakeWordDetector.onWakeWord = { startListening() }
        viewModelScope.launch {
            settings.wakeWordEnabled.collect { enabled ->
                if (enabled) wakeWordDetector.arm() else wakeWordDetector.disarm()
            }
        }
        // Clap: start/stop detector from Settings. On double clap, listen.
        viewModelScope.launch {
            combine(
                settings.clapEnabled, settings.clapThreshold
            ) { enabled, threshold -> enabled to threshold }
                .collect { (enabled, threshold) ->
                    if (enabled) {
                        clapDetector?.stop()
                        clapDetector = ClapDetector(
                            scope = viewModelScope,
                            threshold = threshold,
                            onClap = { startListening() }
                        ).also { it.start() }
                    } else {
                        clapDetector?.stop()
                        clapDetector = null
                    }
                }
        }
    }

    fun startListening() {
        _error.value = null
        viewModelScope.launch {
            val locale = settings.localeTag.first()
            voiceSessionManager.startListening(locale)
        }
    }

    fun stop() {
        voiceSessionManager.stop()
    }

    fun recognizerAvailable(): Boolean = speechRecognizerManager.isAvailable()
}

@Composable
fun VoiceScreen(viewModel: VoiceViewModel = hiltViewModel()) {
    val sessionState by viewModel.sessionState.collectAsState()
    val partial by viewModel.partial.collectAsState()
    val transcript by viewModel.lastTranscript.collectAsState()
    val response by viewModel.lastResponse.collectAsState()
    val error by viewModel.error.collectAsState()
    var showRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startListening()
    }

    if (showRationale) {
        PermissionRationaleDialog(
            permissionName = "Microphone",
            rationale = "JARVIS needs microphone access to hear your voice commands. " +
                "Audio is processed on-device when possible and is never recorded or uploaded without your explicit consent. " +
                "You can stop listening at any time.",
            onConfirm = {
                showRationale = false
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            onDismiss = { showRationale = false }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val listening = sessionState == VoiceSessionState.LISTENING
        AudioVisualizer(active = listening)
        Spacer(Modifier.height(16.dp))
        Text(
            text = when (sessionState) {
                VoiceSessionState.LISTENING -> "Listening…"
                VoiceSessionState.SPEAKING -> "Speaking…"
                VoiceSessionState.ERROR -> "Voice unavailable"
                VoiceSessionState.IDLE -> "Tap the microphone to talk"
            },
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(8.dp))
        if (partial.isNotEmpty()) {
            Text(partial, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else if (transcript.isNotEmpty()) {
            Text("You said: $transcript", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        if (response.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(response, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(32.dp))
        if (listening) {
            Button(onClick = { viewModel.stop() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Stop, contentDescription = null)
                Text("Stop listening")
            }
        } else {
            Button(
                onClick = { showRationale = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Mic, contentDescription = null)
                Text("Start listening")
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { viewModel.stop() }, modifier = Modifier.fillMaxWidth()) {
            Text("Stop all voice activity")
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "The microphone indicator is always visible while listening. " +
                "JARVIS never records audio in the background.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
