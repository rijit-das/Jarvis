package com.jarvis.core.common

import java.util.Calendar
import java.util.Locale

/**
 * Parses human durations and clock times into absolute epoch millis.
 * Pure Kotlin (no Android APIs) so it is fully unit-testable.
 *
 * Supported duration forms: "5 minutes", "2 hours", "90 seconds",
 * "1 hour 30 minutes", "in 10 mins", "in an hour".
 * Supported clock forms: "7 pm", "19:30", "at 7:15 am".
 */
object DurationParser {

    private val unitMillis = mapOf(
        "second" to 1_000L, "seconds" to 1_000L, "sec" to 1_000L, "secs" to 1_000L, "s" to 1_000L,
        "minute" to 60_000L, "minutes" to 60_000L, "min" to 60_000L, "mins" to 60_000L, "m" to 60_000L,
        "hour" to 3_600_000L, "hours" to 3_600_000L, "hr" to 3_600_000L, "hrs" to 3_600_000L, "h" to 3_600_000L,
        "day" to 86_400_000L, "days" to 86_400_000L
    )

    /** Returns epoch millis for "in <duration>" relative to [nowMillis], or null. */
    fun parseRelative(text: String, nowMillis: Long): Long? {
        val normalized = text.lowercase(Locale.US)
            .replace("in an hour", "in 1 hour")
            .replace("in a hour", "in 1 hour")
            .replace("in a minute", "in 1 minute")
            .replace("in a second", "in 1 second")
            .replace("in a day", "in 1 day")
        val pattern = Regex("""(\d+)\s*(seconds?|secs?|s|minutes?|mins?|m|hours?|hrs?|h|days?)\b""")
        val matches = pattern.findAll(normalized).toList()
        if (matches.isEmpty()) return null
        var total = 0L
        for (match in matches) {
            val amount = match.groupValues[1].toLongOrNull() ?: return null
            val unit = unitMillis[match.groupValues[2]] ?: return null
            total += amount * unit
        }
        if (total <= 0L) return null
        return nowMillis + total
    }

    /** Returns epoch millis for the next occurrence of a clock time, or null. */
    fun parseClockTime(text: String, nowMillis: Long): Long? {
        val normalized = text.lowercase(Locale.US).trim()
        // 24h form: 19:30
        val h24 = Regex("""\b([01]?\d|2[0-3]):([0-5]\d)\b""").find(normalized)
        if (h24 != null) {
            return nextOccurrence(
                h24.groupValues[1].toInt(), h24.groupValues[2].toInt(), nowMillis
            )
        }
        // 12h form: 7 pm / 7:15 am
        val h12 = Regex("""\b(1[0-2]|0?[1-9])(?::([0-5]\d))?\s*(am|pm)\b""").find(normalized)
        if (h12 != null) {
            var hour = h12.groupValues[1].toInt()
            val minute = if (h12.groupValues[2].isEmpty()) 0 else h12.groupValues[2].toInt()
            val isPm = h12.groupValues[3] == "pm"
            hour = when {
                isPm && hour != 12 -> hour + 12
                !isPm && hour == 12 -> 0
                else -> hour
            }
            return nextOccurrence(hour, minute, nowMillis)
        }
        return null
    }

    private fun nextOccurrence(hour: Int, minute: Int, nowMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= nowMillis) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    /** Human-readable duration, e.g. "5 minutes", "1 hour 30 minutes". */
    fun formatDuration(millis: Long): String {
        if (millis < 60_000L) return "${millis / 1_000} seconds"
        val minutes = millis / 60_000L
        if (minutes < 60L) return "$minutes minutes"
        val hours = minutes / 60L
        val remMin = minutes % 60L
        return if (remMin == 0L) "$hours hours" else "$hours hours $remMin minutes"
    }
}
