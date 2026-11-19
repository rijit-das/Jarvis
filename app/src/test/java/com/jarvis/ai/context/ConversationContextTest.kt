package com.jarvis.ai.context

import com.jarvis.ai.intent.Intent
import com.jarvis.ai.intent.IntentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConversationContextTest {

    @Test
    fun `repeat resolves to previous actionable intent`() {
        val context = ConversationContext()
        val openCamera = Intent(IntentType.OPEN_APP, "Open camera", mapOf("app" to "camera"))
        context.recordIntent(openCamera)
        val resolved = context.resolve(Intent(IntentType.REPEAT_LAST, "Open that again"))
        assertEquals(IntentType.OPEN_APP, resolved.type)
        assertEquals("camera", resolved.entities["app"])
    }

    @Test
    fun `repeat with no history stays repeat`() {
        val context = ConversationContext()
        val resolved = context.resolve(Intent(IntentType.REPEAT_LAST, "Do that again"))
        assertEquals(IntentType.REPEAT_LAST, resolved.type)
    }

    @Test
    fun `pending confirmation is one-shot`() {
        val context = ConversationContext()
        context.setPendingConfirmation(
            ConversationContext.PendingConfirmation("forget", mapOf("key" to "x"), "Forget x?")
        )
        assertEquals("forget", context.takePendingConfirmation()?.toolName)
        assertNull(context.takePendingConfirmation())
    }

    @Test
    fun `turns are bounded`() {
        val context = ConversationContext()
        repeat(100) { context.recordTurn("user", "turn $it") }
        assertEquals(50, context.recentTurns(100).size)
    }
}
