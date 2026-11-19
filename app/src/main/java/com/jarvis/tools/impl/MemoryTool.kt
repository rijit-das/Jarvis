package com.jarvis.tools.impl

import com.jarvis.data.repository.MemoryRepository
import com.jarvis.tools.Tool
import com.jarvis.tools.ToolResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers a user-approved fact. Memory writes only happen on explicit user
 * request ("remember that ...") — never silently.
 */
@Singleton
class RememberTool @Inject constructor(
    private val memoryRepository: MemoryRepository
) : Tool {
    override val name = "remember"
    override val description = "remember a fact"
    override val inputSchema = mapOf("fact" to "Fact text, e.g. 'my birthday is June 3'")
    override val outputDescription = "Confirmation"
    override val requiredPermission: String? = null
    override val requiresConfirmation = false
    override val timeoutMs = 10_000L

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val fact = arguments["fact"]?.trim().orEmpty()
        if (fact.isEmpty()) {
            return ToolResult.Failure(spoken = "What should I remember?", reason = "missing fact")
        }
        // Derive a stable key from the first few words.
        val key = fact.lowercase().split(Regex("\\s+")).take(3).joinToString("_")
        memoryRepository.upsert("FACT", key, fact)
        return ToolResult.Success(spoken = "I'll remember that.")
    }
}

/** Recalls stored facts matching a query. */
@Singleton
class RecallTool @Inject constructor(
    private val memoryRepository: MemoryRepository
) : Tool {
    override val name = "recall"
    override val description = "recall a remembered fact"
    override val inputSchema = mapOf("query" to "Search keywords")
    override val outputDescription = "Spoken matching facts"
    override val requiredPermission: String? = null
    override val requiresConfirmation = false
    override val timeoutMs = 10_000L

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val query = arguments["query"]?.trim().orEmpty()
        val results = if (query.isEmpty()) {
            memoryRepository.search("").take(5)
        } else {
            memoryRepository.search(query)
        }
        if (results.isEmpty()) {
            return ToolResult.Success(spoken = "I don't have anything stored about that.")
        }
        val spoken = results.take(3).joinToString(". ") { it.value }
        return ToolResult.Success(spoken = "Here's what I remember: $spoken.")
    }
}

/** Forgets a stored fact by key/query. Requires confirmation (destructive). */
@Singleton
class ForgetTool @Inject constructor(
    private val memoryRepository: MemoryRepository
) : Tool {
    override val name = "forget"
    override val description = "forget a remembered fact"
    override val inputSchema = mapOf("key" to "Fact key or keywords")
    override val outputDescription = "Confirmation"
    override val requiredPermission: String? = null
    override val requiresConfirmation = true
    override val timeoutMs = 10_000L

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val key = arguments["key"]?.trim().orEmpty()
        if (key.isEmpty()) {
            return ToolResult.Failure(spoken = "What should I forget?", reason = "missing key")
        }
        val matches = memoryRepository.search(key)
        if (matches.isEmpty()) {
            return ToolResult.Success(spoken = "I don't have anything stored about that.")
        }
        matches.forEach { memoryRepository.deleteById(it.id) }
        return ToolResult.Success(spoken = "Forgotten.")
    }
}
