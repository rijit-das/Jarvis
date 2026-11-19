package com.jarvis.ui.screens

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.jarvis.ai.model.CapabilityTier
import com.jarvis.ai.model.LocalLlmStatus
import com.jarvis.ai.model.ModelManager
import com.jarvis.core.permissions.PermissionManager
import com.jarvis.voice.SpeechRecognizerManager
import com.jarvis.voice.TextToSpeechManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    val modelManager: ModelManager,
    val permissionManager: PermissionManager,
    val speechRecognizerManager: SpeechRecognizerManager,
    val textToSpeechManager: TextToSpeechManager
) : ViewModel()

@Composable
fun DiagnosticsScreen(viewModel: DiagnosticsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var report by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        report = buildReport(context, viewModel)
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)
    ) {
        Text("Diagnostics", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Device, microphone, speech, storage, and permission status. " +
                "Use this when troubleshooting.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(report, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun buildReport(context: Context, viewModel: DiagnosticsViewModel): String {
    val device = viewModel.modelManager.evaluate()
    val tierText = when (device.tier) {
        CapabilityTier.LOW -> "Low"
        CapabilityTier.MEDIUM -> "Medium"
        CapabilityTier.HIGH -> "High"
    }
    val llmText = when (val status = device.llmStatus) {
        is LocalLlmStatus.NotInstalled -> "Not installed (intent engine in use)"
        is LocalLlmStatus.Ready -> "Ready"
        is LocalLlmStatus.Incompatible -> "Incompatible: ${status.reason}"
    }
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo()
    manager.getMemoryInfo(memoryInfo)
    val stat = StatFs(context.filesDir.path)
    val freeStorageMb = stat.availableBytes / (1024 * 1024)
    val totalRamMb = device.totalMemoryBytes / (1024 * 1024)

    return buildString {
        appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("Android: API ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
        appendLine("Capability tier: $tierText")
        appendLine("On-device LLM: $llmText")
        appendLine("Total RAM: $totalRamMb MB")
        appendLine("Free storage: $freeStorageMb MB")
        appendLine("Microphone permission: ${if (viewModel.permissionManager.hasMicrophone()) "granted" else "denied"}")
        appendLine("Speech recognizer available: ${viewModel.speechRecognizerManager.isAvailable()}")
        appendLine("TTS engine ready: ${viewModel.textToSpeechManager.isReady()}")
        appendLine("Notification access enabled: ${viewModel.permissionManager.isNotificationListenerEnabled()}")
    }
}
