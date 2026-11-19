package com.jarvis.tools.impl

import com.jarvis.core.permissions.PermissionManager
import com.jarvis.notifications.JarvisNotificationListener
import com.jarvis.tools.Tool
import com.jarvis.tools.ToolResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads recent notifications. Requires the user to have manually enabled
 * JARVIS in system notification-access settings — JARVIS cannot enable it
 * programmatically. Notification text is treated as untrusted data.
 */
@Singleton
class NotificationTool @Inject constructor(
    private val permissionManager: PermissionManager
) : Tool {
    override val name = "read_notifications"
    override val description = "read recent notifications"
    override val inputSchema = emptyMap<String, String>()
    override val outputDescription = "Spoken summary of recent notifications"
    override val requiredPermission: String? = null
    override val requiresConfirmation = true
    override val timeoutMs = 10_000L

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        if (!permissionManager.isNotificationListenerEnabled()) {
            return ToolResult.Failure(
                spoken = "Notification access isn't enabled. Enable JARVIS in system Settings under Notification access, then ask again.",
                reason = "notification listener not enabled"
            )
        }
        val recent = JarvisNotificationListener.recentNotifications()
        if (recent.isEmpty()) {
            return ToolResult.Success(spoken = "No recent notifications.")
        }
        val spoken = recent.take(3).joinToString(". ") { "${it.app}: ${it.text.take(80)}" }
        return ToolResult.Success(spoken = "Recent notifications: $spoken.")
    }
}
