package com.jarvis.tools.impl

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.jarvis.core.common.DurationParser
import com.jarvis.data.local.ReminderDao
import com.jarvis.data.local.ReminderEntity
import com.jarvis.tools.Tool
import com.jarvis.tools.ToolResult
import com.jarvis.workers.ReminderReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Creates a persisted reminder: stored in the encrypted DB (survives reboot)
 * and scheduled via AlarmManager. Falls back to inexact delivery when exact
 * alarms are unavailable, and says so.
 */
@Singleton
class ReminderTool @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderDao: ReminderDao
) : Tool {
    override val name = "set_reminder"
    override val description = "set a reminder"
    override val inputSchema = mapOf(
        "message" to "What to remind about",
        "duration" to "Relative duration, e.g. '10 minutes'",
        "time" to "Clock time, e.g. '7 pm'"
    )
    override val outputDescription = "Confirmation with scheduled time"
    override val requiredPermission: String? = null
    override val requiresConfirmation = false
    override val timeoutMs = 10_000L

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val message = arguments["message"]?.trim().orEmpty()
        if (message.isEmpty()) {
            return ToolResult.Failure(
                spoken = "What should I remind you about?",
                reason = "missing reminder message"
            )
        }
        val now = System.currentTimeMillis()
        val triggerAt = when {
            !arguments["duration"].isNullOrBlank() ->
                DurationParser.parseRelative("in ${arguments["duration"]}", now)
            !arguments["time"].isNullOrBlank() ->
                DurationParser.parseClockTime(arguments["time"]!!, now)
            else -> null
        } ?: return ToolResult.Failure(
            spoken = "When should I remind you? Say something like 'in 10 minutes' or 'at 7 pm'.",
            reason = "missing/unparseable when"
        )

        val id = reminderDao.insert(
            ReminderEntity(text = message, triggerAtMillis = triggerAt)
        )
        val exact = scheduleAlarm(id.toInt(), triggerAt, message)
        val whenText = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US)
            .format(java.util.Date(triggerAt))
        return ToolResult.Success(
            spoken = if (exact) {
                "Reminder set for $whenText: $message."
            } else {
                "Reminder set for around $whenText: $message. Exact alarms are off, so timing may be approximate."
            }
        )
    }

    /** Returns true if an exact alarm was scheduled. */
    private fun scheduleAlarm(reminderId: Int, triggerAtMillis: Long, message: String): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_MESSAGE, message)
            putExtra(ReminderReceiver.EXTRA_REMINDER_ID, reminderId.toLong())
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, reminderId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
            )
        } else {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent
            )
        }
        return canExact
    }
}
