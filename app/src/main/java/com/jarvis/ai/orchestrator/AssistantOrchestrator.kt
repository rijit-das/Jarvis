package com.jarvis.ai.orchestrator

import com.jarvis.ai.context.ConversationContext
import com.jarvis.ai.intent.IntentEngine
import com.jarvis.ai.intent.IntentType
import com.jarvis.ai.provider.OnlineProvider
import com.jarvis.core.security.PromptInjectionGuard
import com.jarvis.data.repository.ActivityRepository
import com.jarvis.tools.ToolExecutor
import com.jarvis.tools.ToolResult
import javax.inject.Inject
import javax.inject.Singleton

/** Outcome of handling one user utterance. */
data class AssistantResponse(
    val spoken: String,
    val displayed: String = spoken,
    val usedOnline: Boolean = false,
    val needsClarification: Boolean = false
)

/**
 * The request pipeline:
 * sanitise → classify → resolve context → route to tool / direct answer /
 * online provider → record turn.
 *
 * Honest by design: unknown intents produce an explanation + supported
 * commands, never a fabricated result.
 */
@Singleton
class AssistantOrchestrator @Inject constructor(
    private val intentEngine: IntentEngine,
    private val context: ConversationContext,
    private val toolExecutor: ToolExecutor,
    private val onlineProvider: OnlineProvider,
    private val activityRepository: ActivityRepository
) {
    suspend fun handleUserText(rawText: String): AssistantResponse {
        val text = PromptInjectionGuard.sanitise(rawText)
        context.recordTurn("user", text)

        // Pending confirmation answers ("yes"/"no") are handled by the UI
        // layer via ConversationContext; here we just classify fresh input.
        val classified = intentEngine.classify(text)
        val intent = context.resolve(classified)
        context.recordIntent(intent)

        val response = when (intent.type) {
            IntentType.GREETING -> AssistantResponse("Hello. How can I help?")
            IntentType.FAREWELL -> AssistantResponse("Goodbye.")
            IntentType.THANK_YOU -> AssistantResponse("You're welcome.")
            IntentType.HELP -> AssistantResponse(
                spoken = "I can help with time, timers, reminders, notes, tasks, opening apps, " +
                    "media control, navigation, and remembering facts. Try: " +
                    intentEngine.supportedCommands().take(3).joinToString(". "),
                needsClarification = true
            )
            IntentType.UNKNOWN -> handleUnknown(text)
            else -> routeToTool(toolNameFor(intent.type), intent.entities)
        }

        context.recordTurn("assistant", response.spoken)
        activityRepository.recordTurn("user", text)
        activityRepository.recordTurn("assistant", response.spoken)
        return response
    }

    private suspend fun handleUnknown(text: String): AssistantResponse {
        // Only route to the online provider when every gate passes.
        val gate = onlineProvider.gateReason()
        if (gate == null) {
            val result = onlineProvider.respond(text)
            val answer = result.getOrNull()
            if (answer != null) {
                return AssistantResponse(spoken = answer, usedOnline = true)
            }
        }
        val reason = gate ?: "The online provider couldn't answer that."
        return AssistantResponse(
            spoken = "I don't know how to do that yet. $reason " +
                "Offline, I can help with time, reminders, notes, tasks, apps, media, and navigation. " +
                "Say 'help' for examples.",
            needsClarification = true
        )
    }

    /** Explicit intent → tool mapping (tool names are stable API). */
    private fun toolNameFor(type: IntentType): String = when (type) {
        IntentType.TIME_NOW -> "time_now"
        IntentType.DATE_NOW -> "date_now"
        IntentType.SET_TIMER -> "set_timer"
        IntentType.SET_REMINDER -> "set_reminder"
        IntentType.CREATE_NOTE -> "create_note"
        IntentType.LIST_NOTES -> "list_notes"
        IntentType.SEARCH_NOTES -> "search_notes"
        IntentType.ADD_TASK -> "add_task"
        IntentType.LIST_TASKS -> "list_tasks"
        IntentType.COMPLETE_TASK -> "complete_task"
        IntentType.OPEN_APP -> "open_app"
        IntentType.PLAY_MEDIA -> "media_play"
        IntentType.PAUSE_MEDIA -> "media_pause"
        IntentType.NEXT_TRACK -> "media_next"
        IntentType.PREVIOUS_TRACK -> "media_previous"
        IntentType.NAVIGATE -> "navigate"
        IntentType.READ_NOTIFICATIONS -> "read_notifications"
        IntentType.REMEMBER -> "remember"
        IntentType.RECALL -> "recall"
        IntentType.FORGET -> "forget"
        else -> "unknown"
    }

    private suspend fun routeToTool(toolName: String, entities: Map<String, String>): AssistantResponse {
        val result = toolExecutor.execute(toolName, entities)
        return when (result) {
            is ToolResult.Success -> AssistantResponse(spoken = result.spoken)
            is ToolResult.NeedsConfirmation -> AssistantResponse(
                spoken = result.description,
                needsClarification = true
            )
            is ToolResult.Failure -> AssistantResponse(
                spoken = result.spoken,
                needsClarification = true
            )
        }
    }
}
