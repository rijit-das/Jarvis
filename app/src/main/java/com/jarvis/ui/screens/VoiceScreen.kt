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
import com.jarvis.ui.components.CoreState
import com.jarvis.ui.components.GlassCard
import com.jarvis.ui.components.JarvisCore
import com.jarvis.ui.components.PermissionRationaleDialog
import com.jarvis.voice.SpeechRecognizerManager
import com.jarvis.voice.VoiceActivationManager
import com.jarvis.voice.VoiceSessionManager
import com.jarvis.voice.VoiceSessionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class VoiceViewModel @Inject constructor(
    private val voiceSessionManager: VoiceSessionManager,
    private val speechRecognizerManager: SpeechRecognizerManager,
    private val voiceActivationManager: VoiceActivationManager
) : ViewModel() {

    val sessionState: StateFlow<VoiceSessionState> = voiceSessionManager.state
    val partial = speechRecognizerManager.partial
    val error = voiceSessionManager.error

    // Transcript/response are owned by the app-scoped activation manager so
    // wake-word and clap triggers work from any screen.
    val lastTranscript: StateFlow<String> = voiceActivationManager.lastTranscript
    val lastResponse: StateFlow<String> = voiceActivationManager.lastResponse

    fun startListening() {
        voiceActivationManager.startSession()
    }

    fun stop() {
        voiceActivationManager.stopSession()
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
        val coreState = when (sessionState) {
            VoiceSessionState.LISTENING -> CoreState.LISTENING
            VoiceSessionState.SPEAKING -> CoreState.SPEAKING
            VoiceSessionState.ERROR -> CoreState.ERROR
            VoiceSessionState.IDLE -> CoreState.IDLE
        }
        JarvisCore(state = coreState, interactive = false)
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
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(partial, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else if (transcript.isNotEmpty()) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text("You said: $transcript", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        if (response.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(response, style = MaterialTheme.typography.bodyLarge)
            }
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
