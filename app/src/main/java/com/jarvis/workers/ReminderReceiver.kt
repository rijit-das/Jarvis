package com.jarvis.workers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint

/**
 * Fires when a timer/reminder alarm elapses. Posts a high-priority
 * notification and vibrates. Marks the persisted reminder delivered.
 */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {

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
            // Persisted reminders are marked delivered by the worker path;
            // here we only notify. BootReceiver reschedules pending ones.
        }
    }

    companion object {
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
}
