package com.jarvis.ai.orchestrator

import com.jarvis.ai.context.ConversationContext
import com.jarvis.ai.intent.IntentEngine
import com.jarvis.ai.provider.OnlineProvider
import com.jarvis.data.repository.ActivityRepository
import com.jarvis.tools.ToolExecutor
import com.jarvis.tools.ToolResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

/**
 * Orchestrator routing tests. The intent engine and context are real;
 * collaborators are mocked with mockito-kotlin (suspend-aware).
 */
class AssistantOrchestratorTest {

    private fun orchestrator(
        toolExecutor: ToolExecutor,
        onlineProvider: OnlineProvider
    ): AssistantOrchestrator {
        val activityRepository = mock<ActivityRepository>().stub {
            onBlocking { recordTurn(any(), any()) } doReturn Unit
        }
        return AssistantOrchestrator(
            intentEngine = IntentEngine(),
            context = ConversationContext(),
            toolExecutor = toolExecutor,
            onlineProvider = onlineProvider,
            activityRepository = activityRepository
        )
    }

    @Test
    fun `greeting is answered without tools`() = runTest {
        val toolExecutor = mock<ToolExecutor>()
        val onlineProvider = mock<OnlineProvider>()
        val response = orchestrator(toolExecutor, onlineProvider).handleUserText("Hello")
        assertEquals("Hello. How can I help?", response.spoken)
        assertFalse(response.usedOnline)
    }

    @Test
    fun `time request routes to the time tool`() = runTest {
        val toolExecutor = mock<ToolExecutor>().stub {
            onBlocking { execute(any(), any()) } doReturn ToolResult.Success("It's 3:00 PM.")
        }
        val onlineProvider = mock<OnlineProvider>()
        val response = orchestrator(toolExecutor, onlineProvider).handleUserText("What time is it?")
        assertEquals("It's 3:00 PM.", response.spoken)
    }

    @Test
    fun `media intent routes to media tool names`() = runTest {
        // Regression: routing used to derive names from the enum, producing
        // "play_media" while the registry declares "media_play".
        val toolExecutor = mock<ToolExecutor>().stub {
            onBlocking { execute(any(), any()) } doReturn ToolResult.Success("Playing.")
        }
        val onlineProvider = mock<OnlineProvider>()
        val response = orchestrator(toolExecutor, onlineProvider).handleUserText("play some music")
        assertEquals("Playing.", response.spoken)
    }

    @Test
    fun `unknown intent explains limits when online is gated`() = runTest {
        val toolExecutor = mock<ToolExecutor>()
        val onlineProvider = mock<OnlineProvider>().stub {
            onBlocking { gateReason() } doReturn "Offline mode is on."
        }
        val response = orchestrator(toolExecutor, onlineProvider).handleUserText("Do something weird")
        assertTrue(response.spoken.contains("don't know how to do that"))
        assertTrue(response.needsClarification)
        assertFalse(response.usedOnline)
    }

    @Test
    fun `unknown intent uses online provider when gates pass`() = runTest {
        val toolExecutor = mock<ToolExecutor>()
        val onlineProvider = mock<OnlineProvider>().stub {
            onBlocking { gateReason() } doReturn null
            onBlocking { respond(any()) } doReturn Result.success("Online answer")
        }
        val response = orchestrator(toolExecutor, onlineProvider).handleUserText("Tell me a joke")
        assertEquals("Online answer", response.spoken)
        assertTrue(response.usedOnline)
    }
}
