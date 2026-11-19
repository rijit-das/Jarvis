package com.jarvis.workers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.jarvis.data.local.ReminderDao
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Re-registers pending reminders after reboot. Runs on a background scope;
 * the app's Hilt graph provides the DAO.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var reminderDao: ReminderDao

    @Inject
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        scope.launch(Dispatchers.IO) {
            try {
                val pending = reminderDao.pending()
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val now = System.currentTimeMillis()
                for (reminder in pending) {
                    if (reminder.triggerAtMillis <= now) continue
                    val alarmIntent = Intent(context, ReminderReceiver::class.java).apply {
                        putExtra(ReminderReceiver.EXTRA_MESSAGE, reminder.text)
                        putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context, reminder.id.toInt(), alarmIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                        alarmManager.canScheduleExactAlarms()
                    if (canExact) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, reminder.triggerAtMillis, pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, reminder.triggerAtMillis, pendingIntent
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
