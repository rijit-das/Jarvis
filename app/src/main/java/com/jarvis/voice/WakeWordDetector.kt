package com.jarvis.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** Wake-word detector state. */
enum class WakeWordState { DISABLED, ARMED, TRIGGERED }

/**
 * Wake-word ("Hey JARVIS") detection.
 *
 * Android has no built-in offline hotword API, so this implementation uses
 * the device's SpeechRecognizer in a foreground-service session, listening
 * for the phrase. It is:
 * - opt-in (disabled by default),
 * - foreground-bound (visible notification + stop control),
 * - honest about its limits (accuracy/battery vary by device/recognizer).
 *
 * A dedicated on-device hotword model (e.g. Porcupine, user-supplied key) is
 * a documented roadmap item — see docs/KNOWN_LIMITATIONS.md.
 */
@Singleton
class WakeWordDetector @Inject constructor(
    private val speechRecognizerManager: SpeechRecognizerManager
) {
    private val _state = MutableStateFlow(WakeWordState.DISABLED)
    val state: StateFlow<WakeWordState> = _state.asStateFlow()

    var onWakeWord: (() -> Unit)? = null

    private var armed = false

    fun arm() {
        armed = true
        _state.value = WakeWordState.ARMED
        speechRecognizerManager.onResult = { transcript ->
            if (armed && isWakePhrase(transcript)) {
                _state.value = WakeWordState.TRIGGERED
                onWakeWord?.invoke()
            }
        }
        speechRecognizerManager.startListening()
    }

    fun disarm() {
        armed = false
        _state.value = WakeWordState.DISABLED
        speechRecognizerManager.stopListening()
    }

    fun isWakePhrase(transcript: String): Boolean {
        val normalized = transcript.lowercase().trim()
        return normalized.startsWith("hey jarvis") ||
            normalized.startsWith("hey jarvis") ||
            normalized.contains("hey jarvis")
    }
}
