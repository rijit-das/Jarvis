package com.jarvis.voice

import com.jarvis.core.permissions.PermissionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Voice session lifecycle state. */
enum class VoiceSessionState { IDLE, LISTENING, SPEAKING, ERROR }

/**
 * Owns the microphone lifecycle. The mic is held ONLY during an active
 * session and released immediately when the session ends. All mic access
 * goes through here — nothing else in the app touches the microphone.
 */
@Singleton
class VoiceSessionManager @Inject constructor(
    private val speechRecognizerManager: SpeechRecognizerManager,
    private val textToSpeechManager: TextToSpeechManager,
    private val permissionManager: PermissionManager,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(VoiceSessionState.IDLE)
    val state: StateFlow<VoiceSessionState> = _state.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    var onTranscript: ((String) -> Unit)? = null

    fun startListening(localeTag: String = "en-US") {
        if (!permissionManager.hasMicrophone()) {
            _state.value = VoiceSessionState.ERROR
            _error.value = "Microphone permission is required for voice input."
            return
        }
        _error.value = null
        _state.value = VoiceSessionState.LISTENING
        speechRecognizerManager.onResult = { transcript ->
            _state.value = VoiceSessionState.IDLE
            onTranscript?.invoke(transcript)
        }
        speechRecognizerManager.onError = { message ->
            _state.value = VoiceSessionState.ERROR
            _error.value = message
        }
        speechRecognizerManager.startListening(localeTag)
    }

    fun speak(text: String) {
        _state.value = VoiceSessionState.SPEAKING
        textToSpeechManager.speak(text)
        scope.launch {
            // TTS completion is best-effort; return to idle shortly after.
            kotlinx.coroutines.delay(500)
            if (_state.value == VoiceSessionState.SPEAKING) {
                _state.value = VoiceSessionState.IDLE
            }
        }
    }

    /** Stops listening/speaking and releases the microphone. */
    fun stop() {
        speechRecognizerManager.stopListening()
        textToSpeechManager.stop()
        _state.value = VoiceSessionState.IDLE
    }
}
