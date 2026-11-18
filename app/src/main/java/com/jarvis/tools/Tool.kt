package com.jarvis.tools

/**
 * Controlled tool-calling contract. Every tool declares:
 * - a stable [name]
 * - a documented input/output schema ([inputSchema] / [outputDescription])
 * - [requiredPermission] (Android permission or null)
 * - whether [requiresConfirmation] is needed before execution
 * - an execution [timeoutMs]
 *
 * Tools never receive shell access or unrestricted device control.
 */
interface Tool {
    val name: String
    val description: String
    val inputSchema: Map<String, String>
    val outputDescription: String
    val requiredPermission: String?
    val requiresConfirmation: Boolean
    val timeoutMs: Long

    suspend fun execute(arguments: Map<String, String>): ToolResult
}

/** Result of a tool execution. [spoken] is what JARVIS says aloud. */
sealed interface ToolResult {
    data class Success(val spoken: String, val detail: String = spoken) : ToolResult
    data class NeedsConfirmation(val description: String) : ToolResult
    data class Failure(val spoken: String, val reason: String = spoken) : ToolResult
}
