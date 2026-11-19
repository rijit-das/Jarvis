package com.jarvis.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sin

class ClapDetectorTest {

    private val detector = ClapDetector(
        scope = CoroutineScope(Dispatchers.Unconfined),
        threshold = 0.35f,
        onClap = {}
    )

    @Test
    fun `silence is not a clap`() {
        val silence = ShortArray(2048) { 0 }
        assertFalse(detector.isTransient(silence, silence.size))
    }

    @Test
    fun `low sine tone is not a clap`() {
        // A pure low-frequency sine has a low zero-crossing rate.
        val tone = ShortArray(2048) { i ->
            (sin(i * 0.01) * 20000).toInt().toShort()
        }
        assertFalse(detector.isTransient(tone, tone.size))
    }

    @Test
    fun `loud broadband noise burst is a transient`() {
        // Alternating high-amplitude samples: high RMS + high ZCR.
        val burst = ShortArray(2048) { i ->
            if (i % 2 == 0) 25000.toShort() else (-25000).toShort()
        }
        assertTrue(detector.isTransient(burst, burst.size))
    }

    @Test
    fun `quiet noise is not a clap`() {
        val quiet = ShortArray(2048) { i ->
            if (i % 2 == 0) 100.toShort() else (-100).toShort()
        }
        assertFalse(detector.isTransient(quiet, quiet.size))
    }
}
