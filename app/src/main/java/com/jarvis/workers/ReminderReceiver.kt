package com.jarvis.workers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.jarvis.data.local.ReminderDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fires when a timer/reminder alarm elapses. Posts a high-priority
 * notification, vibrates, and marks the persisted reminder delivered so it
 * is never re-fired after a reboot.
 */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderDao: ReminderDao

    @Inject
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "Reminder"
        val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)

        val notification = NotificationCompat.Builder(context, "reminders")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("JARVIS reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(reminderId.toInt().coerceAtLeast(1), notification)

        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrator?.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))

        if (reminderId >= 0) {
            val pendingResult = goAsync()
            scope.launch(Dispatchers.IO) {
                try {
                    reminderDao.markDelivered(reminderId)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
}
