package com.jarvis.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock

class WakeWordDetectorTest {

    private val detector = WakeWordDetector(
        speechRecognizerManager = mock(),
        scope = CoroutineScope(Dispatchers.Unconfined)
    )

    @Test
    fun `detects exact wake phrase`() {
        assertTrue(detector.isWakePhrase("Hey JARVIS"))
        assertTrue(detector.isWakePhrase("hey jarvis"))
    }

    @Test
    fun `detects wake phrase within a sentence`() {
        assertTrue(detector.isWakePhrase("please hey jarvis open camera"))
    }

    @Test
    fun `rejects non-wake phrases`() {
        assertFalse(detector.isWakePhrase("hello"))
        assertFalse(detector.isWakePhrase("open camera"))
        assertFalse(detector.isWakePhrase("jarvis"))
        assertFalse(detector.isWakePhrase(""))
    }
}
