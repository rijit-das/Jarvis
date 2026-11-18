package com.jarvis.data.repository

import com.jarvis.data.local.AuditLogDao
import com.jarvis.data.local.AuditLogEntity
import com.jarvis.data.local.ConversationDao
import com.jarvis.data.local.ConversationTurnEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Repository for conversation history + audit log. */
@Singleton
class ActivityRepository @Inject constructor(
    private val conversationDao: ConversationDao,
    private val auditLogDao: AuditLogDao
) {
    suspend fun recordTurn(role: String, text: String) {
        conversationDao.insert(
            ConversationTurnEntity(
                role = role,
                text = text,
                timestampMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun recentTurns(limit: Int = 50): List<ConversationTurnEntity> =
        conversationDao.recent(limit)

    fun observeAudit(limit: Int = 200): Flow<List<AuditLogEntity>> =
        auditLogDao.observeRecent(limit)

    suspend fun clearConversation() = conversationDao.deleteAll()

    suspend fun clearAudit() = auditLogDao.deleteAll()
}
