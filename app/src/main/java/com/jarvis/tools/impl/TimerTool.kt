package com.jarvis.tools.impl

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.jarvis.core.common.DurationParser
import com.jarvis.tools.Tool
import com.jarvis.tools.ToolResult
import com.jarvis.workers.ReminderReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sets a timer via AlarmManager. Fires a high-priority notification when it
 * elapses. Falls back to inexact delivery if exact alarms are unavailable.
 */
@Singleton
class TimerTool @Inject constructor(
    @ApplicationContext private val context: Context
) : Tool {
    override val name = "set_timer"
    override val description = "set a timer"
    override val inputSchema = mapOf("duration" to "Duration phrase, e.g. '5 minutes'")
    override val outputDescription = "Confirmation with remaining duration"
    override val requiredPermission: String? = null
    override val requiresConfirmation = false
    override val timeoutMs = 10_000L

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val durationText = arguments["duration"]?.trim().orEmpty()
        if (durationText.isEmpty()) {
            return ToolResult.Failure(
                spoken = "How long should the timer be? For example, say 'set a timer for 5 minutes'.",
                reason = "missing duration"
            )
        }
        val triggerAt = DurationParser.parseRelative("in $durationText", System.currentTimeMillis())
            ?: return ToolResult.Failure(
                spoken = "I couldn't understand that duration. Try '5 minutes' or '1 hour'.",
                reason = "unparseable duration: $durationText"
            )
        val durationMs = triggerAt - System.currentTimeMillis()
        // Unique request code per timer so multiple timers don't overwrite each other.
        scheduleAlarm(triggerAt, "Timer finished.", requestCode = (System.currentTimeMillis() % Int.MAX_VALUE).toInt())
        return ToolResult.Success(
            spoken = "Timer set for ${DurationParser.formatDuration(durationMs)}. I'll notify you when it's done."
        )
    }

    private fun scheduleAlarm(triggerAtMillis: Long, message: String, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_MESSAGE, message)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
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
    }
}
