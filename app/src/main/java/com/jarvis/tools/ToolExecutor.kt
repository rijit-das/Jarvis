package com.jarvis.tools

import com.jarvis.core.common.DispatcherProvider
import com.jarvis.core.permissions.PermissionManager
import com.jarvis.core.security.AuditLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executes tools under policy:
 * 1. Allowlist check (registry).
 * 2. Permission check (Android permission must be granted).
 * 3. Confirmation check (consequential tools need explicit user approval).
 * 4. Timeout + audit logging.
 *
 * Security policy lives here in code — it cannot be overridden by
 * conversational instructions.
 */
@Singleton
class ToolExecutor @Inject constructor(
    private val registry: ToolRegistry,
    private val permissionManager: PermissionManager,
    private val auditLogger: AuditLogger,
    private val dispatchers: DispatcherProvider
) {
    /** Set by the UI layer to request user confirmation. */
    var confirmationHandler: (suspend (description: String) -> Boolean)? = null

    suspend fun execute(toolName: String, arguments: Map<String, String>): ToolResult {
        val tool = registry.get(toolName)
            ?: return ToolResult.Failure(
                spoken = "I don't have a tool called $toolName.",
                reason = "unknown tool: $toolName"
            )

        tool.requiredPermission?.let { permission ->
            if (!permissionManager.isGranted(permission)) {
                auditLogger.permissionDecision(permission, false)
                return ToolResult.Failure(
                    spoken = "I need a permission to do that. Please grant it in the Permissions screen.",
                    reason = "missing permission: $permission"
                )
            }
        }

        if (tool.requiresConfirmation) {
            val handler = confirmationHandler
            val description = "Allow JARVIS to ${tool.description}?"
            if (handler == null) {
                return ToolResult.NeedsConfirmation(description)
            }
            val approved = handler(description)
            auditLogger.log("confirmation", "tool=$toolName approved=$approved")
            if (!approved) {
                return ToolResult.Failure(spoken = "Cancelled.", reason = "user declined confirmation")
            }
        }

        val startedAt = System.currentTimeMillis()
        return try {
            val result = withContext(dispatchers.default) {
                withTimeout(tool.timeoutMs) { tool.execute(arguments) }
            }
            auditLogger.toolExecuted(toolName, result is ToolResult.Success, System.currentTimeMillis() - startedAt)
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            auditLogger.toolExecuted(toolName, false, System.currentTimeMillis() - startedAt)
            ToolResult.Failure(
                spoken = "Something went wrong while doing that. ${e.message ?: ""}".trim(),
                reason = e.message ?: e.toString()
            )
        }
    }
}
