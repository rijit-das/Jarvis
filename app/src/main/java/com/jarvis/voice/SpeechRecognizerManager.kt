package com.jarvis.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Listening state for the UI. */
enum class ListeningState { IDLE, LISTENING, PROCESSING, ERROR }

/**
 * SpeechRecognizer wrapper with offline preference.
 *
 * Two modes share one recognizer instance (the platform allows only one at a
 * time):
 * - **Manual mode** ([startListening]): results go to [onResult].
 * - **Wake mode** ([startWakeListening]): results go to [onWakeResult] so the
 *   wake-word detector never fights the voice session for the callback.
 *
 * Starting either mode cancels the other. Both request
 * `EXTRA_PREFER_OFFLINE` so on-device recognition is used when available.
 */
@Singleton
class SpeechRecognizerManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _state = MutableStateFlow(ListeningState.IDLE)
    val state: StateFlow<ListeningState> = _state.asStateFlow()

    private val _partial = MutableStateFlow("")
    val partial: StateFlow<String> = _partial.asStateFlow()

    var onResult: ((String) -> Unit)? = null
    var onWakeResult: ((String) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private var recognizer: SpeechRecognizer? = null
    private var wakeMode = false

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    /** Manual voice input. */
    fun startListening(localeTag: String = "en-US") {
        wakeMode = false
        startInternal(localeTag)
    }

    /** Wake-word listening. Results route to [onWakeResult]. */
    fun startWakeListening(localeTag: String = "en-US") {
        wakeMode = true
        startInternal(localeTag)
    }

    private fun startInternal(localeTag: String) {
        if (!isAvailable()) {
            _state.value = ListeningState.ERROR
            onError?.invoke("No speech recognizer is installed on this device. Text input is still available.")
            return
        }
        stopListening()
        _state.value = ListeningState.LISTENING
        _partial.value = ""
        val created = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = created
        created.setRecognitionListener(object : RecognitionListener {
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray) = Unit
            override fun onEndOfSpeech() {
                _state.value = ListeningState.PROCESSING
            }

            override fun onError(error: Int) {
                _state.value = ListeningState.ERROR
                onError?.invoke(errorMessage(error))
            }

            override fun onResults(results: Bundle) {
                _state.value = ListeningState.IDLE
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim().orEmpty()
                if (text.isEmpty()) {
                    onError?.invoke("I didn't catch that. Please try again.")
                } else if (wakeMode) {
                    onWakeResult?.invoke(text)
                } else {
                    onResult?.invoke(text)
                }
            }

            override fun onPartialResults(partialResults: Bundle) {
                val matches = partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                _partial.value = matches?.firstOrNull().orEmpty()
            }

            override fun onEvent(eventType: Int, params: Bundle) = Unit
            override fun onReadyForSpeech(params: Bundle) = Unit
        })
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        try {
            created.startListening(intent)
        } catch (e: Exception) {
            _state.value = ListeningState.ERROR
            onError?.invoke("Couldn't start listening: ${e.message}")
        }
    }

    fun stopListening() {
        // NOTE: wakeMode is intentionally NOT reset here — startInternal()
        // calls stopListening() before setting the mode for the new session.
        try {
            recognizer?.cancel()
            recognizer?.destroy()
        } catch (_: Exception) {
            // Already released.
        }
        recognizer = null
        if (_state.value != ListeningState.ERROR) _state.value = ListeningState.IDLE
    }

    private fun errorMessage(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH -> "I didn't catch that. Please try again."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "I didn't hear anything. Please try again."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required for voice input."
        SpeechRecognizer.ERROR_NETWORK -> "Speech recognition needs a network connection on this device. Text input is still available."
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition timed out. Please try again."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "The recognizer is busy. Please wait a moment and try again."
        else -> "Speech recognition error ($error)."
    }
}
