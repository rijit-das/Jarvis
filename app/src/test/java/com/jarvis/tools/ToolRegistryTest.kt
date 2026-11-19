package com.jarvis.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ToolRegistryTest {

    private fun fakeTool(name: String): Tool = object : Tool {
        override val name = name
        override val description = "fake $name"
        override val inputSchema = emptyMap<String, String>()
        override val outputDescription = "fake output"
        override val requiredPermission: String? = null
        override val requiresConfirmation = false
        override val timeoutMs = 1_000L
        override suspend fun execute(arguments: Map<String, String>): ToolResult =
            ToolResult.Success("ok")
    }

    @Test
    fun `registered tools are retrievable`() {
        val registry = ToolRegistry()
        registry.register(fakeTool("a"))
        registry.register(fakeTool("b"))
        assertNotNull(registry.get("a"))
        assertEquals(2, registry.all().size)
        assertEquals(setOf("a", "b"), registry.names())
    }

    @Test
    fun `unregistered tools are rejected`() {
        val registry = ToolRegistry()
        assertNull(registry.get("shell_exec"))
        assertNull(registry.get("anything_else"))
    }
}
