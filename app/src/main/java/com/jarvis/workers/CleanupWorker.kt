package com.jarvis.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jarvis.data.local.AuditLogDao
import com.jarvis.data.local.ConversationDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodic cleanup: prunes conversation turns and audit entries older than
 * 30 days to bound storage growth.
 */
@HiltWorker
class CleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val conversationDao: ConversationDao,
    private val auditLogDao: AuditLogDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val cutoff = System.currentTimeMillis() - RETENTION_MILLIS
        conversationDao.deleteOlderThan(cutoff)
        auditLogDao.deleteOlderThan(cutoff)
        return Result.success()
    }

    companion object {
        private const val RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000
    }
}
