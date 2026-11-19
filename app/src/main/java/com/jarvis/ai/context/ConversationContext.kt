package com.jarvis.ai.context

import com.jarvis.ai.intent.Intent
import com.jarvis.ai.intent.IntentType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory session context: last intent, last tool result, recent turns.
 * Session context is never persisted — it dies with the process.
 *
 * Resolves references like "open that again" and supplies follow-up context
 * ("yes", "no") to the orchestrator.
 */
@Singleton
class ConversationContext @Inject constructor() {

    data class Turn(val role: String, val text: String, val timestampMillis: Long)

    private val turns = ArrayDeque<Turn>()
    private var lastIntent: Intent? = null
    private var lastToolName: String? = null
    private var pendingConfirmation: PendingConfirmation? = null

    /** A tool call awaiting user confirmation. */
    data class PendingConfirmation(
        val toolName: String,
        val arguments: Map<String, String>,
        val description: String
    )

    @Synchronized
    fun recordTurn(role: String, text: String) {
        turns.addLast(Turn(role, text, System.currentTimeMillis()))
        while (turns.size > MAX_TURNS) turns.removeFirst()
    }

    @Synchronized
    fun recordIntent(intent: Intent) {
        lastIntent = intent
    }

    @Synchronized
    fun recordToolExecution(toolName: String) {
        lastToolName = toolName
    }

    @Synchronized
    fun lastIntent(): Intent? = lastIntent

    @Synchronized
    fun lastToolName(): String? = lastToolName

    @Synchronized
    fun recentTurns(limit: Int = 10): List<Turn> = turns.takeLast(limit)

    @Synchronized
    fun setPendingConfirmation(pending: PendingConfirmation) {
        pendingConfirmation = pending
    }

    @Synchronized
    fun takePendingConfirmation(): PendingConfirmation? {
        val pending = pendingConfirmation
        pendingConfirmation = null
        return pending
    }

    @Synchronized
    fun hasPendingConfirmation(): Boolean = pendingConfirmation != null

    /**
     * Resolves an intent against context. Currently handles REPEAT_LAST by
     * returning the previous actionable intent (if any).
     */
    @Synchronized
    fun resolve(intent: Intent): Intent {
        if (intent.type == IntentType.REPEAT_LAST) {
            val previous = lastIntent
            if (previous != null && previous.type != IntentType.REPEAT_LAST &&
                previous.type != IntentType.UNKNOWN
            ) {
                return previous.copy(rawText = intent.rawText)
            }
        }
        return intent
    }

    @Synchronized
    fun clear() {
        turns.clear()
        lastIntent = null
        lastToolName = null
        pendingConfirmation = null
    }

    companion object {
        private const val MAX_TURNS = 50
    }
}
