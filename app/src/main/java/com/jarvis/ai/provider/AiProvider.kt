package com.jarvis.ai.provider

/**
 * Provider-independent AI interface. The orchestrator only ever talks to this
 * abstraction, so offline, online, and (future) on-device LLM backends are
 * interchangeable.
 */
interface AiProvider {
    val name: String
    val isAvailable: Boolean

    /** Answers free-form conversation (no tool execution). */
    suspend fun respond(prompt: String): Result<String>

    /** Optional: classifies an utterance. Null if unsupported. */
    suspend fun classify(prompt: String): Result<String>? = null
}

/** Offline provider: deterministic, no network, always available. */
class OfflineProvider : AiProvider {
    override val name: String = "offline-intent-engine"
    override val isAvailable: Boolean = true

    override suspend fun respond(prompt: String): Result<String> =
        Result.failure(
            UnsupportedOperationException(
                "The offline engine handles structured commands, not open-ended chat. " +
                    "Try a command like 'What time is it?' or enable online mode for open-ended conversation."
            )
        )
}
