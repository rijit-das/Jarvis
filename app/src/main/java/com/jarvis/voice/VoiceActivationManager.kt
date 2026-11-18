package com.jarvis.voice

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.jarvis.ai.orchestrator.AssistantOrchestrator
import com.jarvis.data.datastore.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-scoped voice coordinator. Started once from `JarvisApp.onCreate` so
 * voice settings apply immediately — without requiring the user to visit the
 * Voice screen first.
 *
 * Responsibilities:
 * - Sync TTS rate/pitch/enabled from Settings.
 * - Arm/disarm wake word and clap detection from Settings.
 * - On wake/clap trigger: start a voice session (with foreground service).
 * - Route transcripts → orchestrator → spoken response.
 * - Re-arm the wake word after a manual session ends.
 */
@Singleton
class VoiceActivationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val wakeWordDetector: WakeWordDetector,
    private val voiceSessionManager: VoiceSessionManager,
    private val textToSpeechManager: TextToSpeechManager,
    private val orchestrator: AssistantOrchestrator,
    private val scope: CoroutineScope
) {
    private val _lastTranscript = MutableStateFlow("")
    val lastTranscript: StateFlow<String> = _lastTranscript.asStateFlow()

    private val _lastResponse = MutableStateFlow("")
    val lastResponse: StateFlow<String> = _lastResponse.asStateFlow()

    private var started = false
    private var localeTag = "en-US"
    private var clapDetector: ClapDetector? = null

    fun start() {
        if (started) return
        started = true

        // Locale sync.
        scope.launch {
            settings.localeTag.collect { localeTag = it }
        }

        // TTS config sync.
        scope.launch {
            combine(
                settings.ttsRate, settings.ttsPitch, settings.voiceOutputEnabled
            ) { rate, pitch, enabled -> Triple(rate, pitch, enabled) }
                .collect { (rate, pitch, enabled) ->
                    textToSpeechManager.configure(rate, pitch, enabled)
                }
        }

        // Wake word arm/disarm.
        wakeWordDetector.onWakeWord = { startSession() }
        scope.launch {
            settings.wakeWordEnabled.collect { enabled ->
                if (enabled) wakeWordDetector.arm() else wakeWordDetector.disarm()
            }
        }

        // Clap detector start/stop.
        scope.launch {
            combine(
                settings.clapEnabled, settings.clapThreshold
            ) { enabled, threshold -> enabled to threshold }
                .collect { (enabled, threshold) ->
                    if (enabled) {
                        clapDetector?.stop()
                        clapDetector = ClapDetector(
                            scope = scope,
                            threshold = threshold,
                            onClap = { startSession() }
                        ).also { it.start() }
                    } else {
                        clapDetector?.stop()
                        clapDetector = null
                    }
                }
        }

        // Re-arm wake word after a manual session ends (recognizer is shared).
        scope.launch {
            voiceSessionManager.state.collect { state ->
                if (state == VoiceSessionState.IDLE && settings.wakeWordEnabled.first()) {
                    wakeWordDetector.arm()
                }
            }
        }

        // Transcript → orchestrator → spoken response.
        scope.launch {
            voiceSessionManager.transcripts.collect { transcript ->
                _lastTranscript.value = transcript
                val response = orchestrator.handleUserText(transcript)
                _lastResponse.value = response.spoken
                if (settings.voiceOutputEnabled.first()) {
                    voiceSessionManager.speak(response.spoken)
                }
            }
        }
    }

    /** Starts a voice session with the foreground service (mic indicator). */
    fun startSession() {
        val intent = Intent(context, VoiceForegroundService::class.java).apply {
            action = VoiceForegroundService.ACTION_START
        }
        try {
            ContextCompat.startForegroundService(context, intent)
        } catch (e: IllegalStateException) {
            // Background start restrictions (Android 12+): fall back to a
            // foreground-only session. The mic still works while the app is
            // visible; wake/clap triggers from background may be deferred.
        }
        voiceSessionManager.startListening(localeTag)
    }

    /** Stops the session and the foreground service. */
    fun stopSession() {
        voiceSessionManager.stop()
        val intent = Intent(context, VoiceForegroundService::class.java).apply {
            action = VoiceForegroundService.ACTION_STOP
        }
        context.stopService(intent)
    }
}
