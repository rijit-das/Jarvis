package com.jarvis.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Wake-word detector state. */
enum class WakeWordState { DISABLED, ARMED, TRIGGERED }

/**
 * Wake-word ("Hey JARVIS") detection.
 *
 * Android has no built-in offline hotword API, so this implementation uses
 * the device's SpeechRecognizer in wake mode, listening for the phrase. The
 * recognizer is one-shot, so the detector re-arms after every result/error —
 * giving continuous (best-effort) coverage while armed.
 *
 * It is:
 * - opt-in (disabled by default),
 * - foreground-bound (visible notification + stop control via the voice
 *   session service),
 * - honest about its limits (accuracy/battery vary by device/recognizer).
 *
 * A dedicated on-device hotword model (e.g. Porcupine, user-supplied key) is
 * a documented roadmap item — see docs/KNOWN_LIMITATIONS.md.
 */
@Singleton
class WakeWordDetector @Inject constructor(
    private val speechRecognizerManager: SpeechRecognizerManager,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow(WakeWordState.DISABLED)
    val state: StateFlow<WakeWordState> = _state.asStateFlow()

    var onWakeWord: (() -> Unit)? = null

    private var armed = false
    private var rearmJob: Job? = null

    fun arm() {
        if (armed) return
        armed = true
        _state.value = WakeWordState.ARMED
        speechRecognizerManager.onWakeResult = wake@{ transcript ->
            if (!armed) return@wake
            if (isWakePhrase(transcript)) {
                _state.value = WakeWordState.TRIGGERED
                onWakeWord?.invoke()
                // Stay triggered until disarm() or arm() is called again.
            } else {
                // Not the wake phrase — keep listening.
                scheduleRearm()
            }
        }
        speechRecognizerManager.startWakeListening()
    }

    fun disarm() {
        armed = false
        rearmJob?.cancel()
        rearmJob = null
        _state.value = WakeWordState.DISABLED
        speechRecognizerManager.onWakeResult = null
        speechRecognizerManager.stopListening()
    }

    /** Re-arm after a non-matching result (recognizer is one-shot). */
    private fun scheduleRearm() {
        rearmJob?.cancel()
        rearmJob = scope.launch {
            delay(REARM_DELAY_MS)
            if (armed && scope.isActive) {
                speechRecognizerManager.startWakeListening()
            }
        }
    }

    fun isWakePhrase(transcript: String): Boolean {
        val normalized = transcript.lowercase().trim()
        return normalized.contains("hey jarvis")
    }

    companion object {
        private const val REARM_DELAY_MS = 300L
    }
}
