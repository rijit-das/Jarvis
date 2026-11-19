package com.jarvis.core.security

import com.jarvis.data.local.AuditLogEntity
import com.jarvis.data.local.AuditLogDao
import com.jarvis.core.common.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Append-only audit trail for tool executions, permission decisions, online
 * requests, and security events. Entries contain metadata only — never
 * secrets, API keys, or full transcripts.
 */
@Singleton
class AuditLogger @Inject constructor(
    private val dao: AuditLogDao,
    private val dispatchers: DispatcherProvider,
    private val scope: CoroutineScope
) {
    fun log(eventType: String, detail: String) {
        scope.launch(dispatchers.io) {
            dao.insert(
                AuditLogEntity(
                    timestampMillis = System.currentTimeMillis(),
                    eventType = eventType,
                    detail = detail.take(MAX_DETAIL_LENGTH)
                )
            )
        }
    }

    fun toolExecuted(toolName: String, success: Boolean, durationMs: Long) {
        log("tool", "tool=$toolName success=$success durationMs=$durationMs")
    }

    fun permissionDecision(permission: String, granted: Boolean) {
        log("permission", "permission=$permission granted=$granted")
    }

    fun onlineRequest(provider: String, success: Boolean) {
        log("online", "provider=$provider success=$success")
    }

    fun securityEvent(message: String) {
        log("security", message)
    }

    companion object {
        private const val MAX_DETAIL_LENGTH = 500
    }
}
