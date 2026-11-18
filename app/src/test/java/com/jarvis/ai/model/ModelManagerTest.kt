package com.jarvis.ai.model

import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock

class ModelManagerTest {

    private val manager = ModelManager(mock())

    private val gb = 1024L * 1024 * 1024

    @Test
    fun `low RAM device is LOW tier`() {
        assertEquals(CapabilityTier.LOW, manager.tierFor(2 * gb, 10 * gb))
    }

    @Test
    fun `low storage device is LOW tier`() {
        assertEquals(CapabilityTier.LOW, manager.tierFor(8 * gb, 1 * gb))
    }

    @Test
    fun `mid RAM device is MEDIUM tier`() {
        assertEquals(CapabilityTier.MEDIUM, manager.tierFor(4 * gb, 10 * gb))
    }

    @Test
    fun `high RAM device is HIGH tier`() {
        assertEquals(CapabilityTier.HIGH, manager.tierFor(8 * gb, 20 * gb))
    }

    @Test
    fun `boundary values`() {
        assertEquals(CapabilityTier.MEDIUM, manager.tierFor(3 * gb, 2 * gb))
        assertEquals(CapabilityTier.HIGH, manager.tierFor(6 * gb, 2 * gb))
    }
}
