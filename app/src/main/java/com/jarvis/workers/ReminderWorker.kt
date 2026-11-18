package com.jarvis.workers

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jarvis.data.local.ReminderDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Fallback reminder delivery via WorkManager (used when exact alarms are
 * unavailable). Delivers any overdue pending reminders and marks them done.
 */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val reminderDao: ReminderDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        val overdue = reminderDao.pending().filter { it.triggerAtMillis <= now }
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        for (reminder in overdue) {
            val notification = NotificationCompat.Builder(applicationContext, "reminders")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("JARVIS reminder")
                .setContentText(reminder.text)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
            manager.notify(reminder.id.toInt(), notification)
            reminderDao.markDelivered(reminder.id)
        }
        return Result.success()
    }
}
