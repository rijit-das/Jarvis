package com.jarvis.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Speaking state for the UI. */
enum class SpeakingState { IDLE, SPEAKING }

/**
 * TextToSpeech wrapper: adjustable rate/pitch, interruptible, queue-based.
 * Respects the user's voice-output toggle — never speaks when disabled.
 */
@Singleton
class TextToSpeechManager @Inject constructor(
    @ApplicationContext private val context: Context
) : TextToSpeech.OnInitListener {

    private val _state = MutableStateFlow(SpeakingState.IDLE)
    val state: StateFlow<SpeakingState> = _state.asStateFlow()

    private var tts: TextToSpeech? = null
    private var ready = false
    private var rate = 1.0f
    private var pitch = 1.0f
    private var enabled = true

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val engine = tts ?: return
            engine.language = Locale.US
            engine.setSpeechRate(rate)
            engine.setPitch(pitch)
            engine.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _state.value = SpeakingState.SPEAKING
                }

                override fun onDone(utteranceId: String?) {
                    _state.value = SpeakingState.IDLE
                }

                override fun onError(utteranceId: String?) {
                    _state.value = SpeakingState.IDLE
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _state.value = SpeakingState.IDLE
                }
            })
            ready = true
        }
    }

    fun configure(rate: Float, pitch: Float, enabled: Boolean) {
        this.rate = rate
        this.pitch = pitch
        this.enabled = enabled
        tts?.setSpeechRate(rate)
        tts?.setPitch(pitch)
        if (!enabled) stop()
    }

    fun speak(text: String) {
        if (!enabled || !ready) return
        val engine = tts ?: return
        _state.value = SpeakingState.SPEAKING
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_utterance")
        // TTS has no reliable completion callback without a listener; the
        // state is informational. stop() always resets it.
        _state.value = SpeakingState.IDLE
    }

    fun stop() {
        tts?.stop()
        _state.value = SpeakingState.IDLE
    }

    fun isReady(): Boolean = ready
}
