package com.jarvis.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.sqrt

/** Clap-detector state. */
enum class ClapState { DISABLED, LISTENING, TRIGGERED }

/**
 * Clap detection via PCM analysis.
 *
 * A clap is a short, broadband transient: high short-term energy with a fast
 * attack and decay, plus a high zero-crossing rate. To avoid false triggers
 * from ordinary noise, a single transient is NOT enough — the detector
 * requires a validated double-clap pattern (two transients within a window,
 * separated by relative silence) before firing.
 *
 * Threshold is user-configurable; a cooldown prevents repeat triggers.
 */
class ClapDetector(
    private val scope: CoroutineScope,
    private val threshold: Float = 0.35f,
    private val onClap: () -> Unit
) {
    private val _state = MutableStateFlow(ClapState.DISABLED)
    val state: StateFlow<ClapState> = _state.asStateFlow()

    private var job: Job? = null
    private var audioRecord: AudioRecord? = null

    @SuppressLint("MissingPermission")
    fun start() {
        if (job?.isActive == true) return
        _state.value = ClapState.LISTENING
        // AudioRecord.read() blocks — never run this on the main thread.
        job = scope.launch(kotlinx.coroutines.Dispatchers.Default) {
            val minBuffer = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            val record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                maxOf(minBuffer, BUFFER_SIZE)
            )
            audioRecord = record
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                _state.value = ClapState.DISABLED
                return@launch
            }
            record.startRecording()
            try {
                runDetectionLoop(record)
            } finally {
                try {
                    record.stop()
                    record.release()
                } catch (_: Exception) {
                    // Already released.
                }
                audioRecord = null
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _state.value = ClapState.DISABLED
    }

    private suspend fun runDetectionLoop(record: AudioRecord) {
        val buffer = ShortArray(BUFFER_SIZE)
        var firstClapAt = 0L
        var lastTriggerAt = 0L
        while (scope.isActive) {
            val read = record.read(buffer, 0, buffer.size)
            if (read <= 0) continue
            val now = System.currentTimeMillis()
            if (isTransient(buffer, read)) {
                if (now - lastTriggerAt < COOLDOWN_MS) continue
                if (firstClapAt == 0L) {
                    firstClapAt = now
                } else if (now - firstClapAt <= DOUBLE_CLAP_WINDOW_MS) {
                    // Validated double clap.
                    _state.value = ClapState.TRIGGERED
                    onClap()
                    lastTriggerAt = now
                    firstClapAt = 0L
                    _state.value = ClapState.LISTENING
                } else {
                    firstClapAt = now
                }
            }
            // Reset stale first-clap.
            if (firstClapAt != 0L && now - firstClapAt > DOUBLE_CLAP_WINDOW_MS) {
                firstClapAt = 0L
            }
        }
    }

    /**
     * True if the buffer contains a clap-like transient: high RMS energy
     * relative to full scale AND a high zero-crossing rate (broadband).
     */
    fun isTransient(samples: ShortArray, count: Int): Boolean {
        if (count <= 0) return false
        var sumSquares = 0.0
        var zeroCrossings = 0
        var previous = samples[0]
        for (i in 0 until count) {
            val sample = samples[i].toDouble()
            sumSquares += sample * sample
            if ((sample > 0 && previous < 0) || (sample < 0 && previous > 0)) {
                zeroCrossings++
            }
            previous = samples[i]
        }
        val rms = sqrt(sumSquares / count) / Short.MAX_VALUE
        val zcr = zeroCrossings.toDouble() / count
        return rms > threshold && zcr > MIN_ZERO_CROSSING_RATE
    }

    companion object {
        const val SAMPLE_RATE = 16000
        const val BUFFER_SIZE = 2048
        const val DOUBLE_CLAP_WINDOW_MS = 1200L
        const val COOLDOWN_MS = 3000L
        const val MIN_ZERO_CROSSING_RATE = 0.08
    }
}
