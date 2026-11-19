package com.jarvis.ai.intent

import com.jarvis.core.common.DurationParser
import java.util.Locale

/**
 * Extracts entities (durations, clock times, app names, quoted text) from
 * raw user input. Pure Kotlin — unit-testable.
 */
object EntityExtractor {

    /** Extracts a duration phrase like "5 minutes" / "1 hour 30 minutes". */
    fun extractDuration(text: String): String? {
        val normalized = text.lowercase(Locale.US)
        val pattern = Regex("""(\d+\s*(?:seconds?|secs?|minutes?|mins?|hours?|hrs?|days?)(?:\s+\d+\s*(?:seconds?|minutes?|hours?|days?))?)""")
        return pattern.find(normalized)?.value?.trim()
    }

    /** Extracts a clock time like "7 pm" / "19:30". */
    fun extractClockTime(text: String): String? {
        val normalized = text.lowercase(Locale.US)
        val h24 = Regex("""\b([01]?\d|2[0-3]):([0-5]\d)\b""").find(normalized)
        if (h24 != null) return h24.value
        val h12 = Regex("""\b(1[0-2]|0?[1-9])(?::([0-5]\d))?\s*(am|pm)\b""").find(normalized)
        return h12?.value
    }

    /** Extracts quoted text (single or double quotes). */
    fun extractQuoted(text: String): String? {
        val double = Regex(""""([^"]+)"""").find(text)
        if (double != null) return double.groupValues[1]
        val single = Regex("""'([^']+)'""").find(text)
        return single?.groupValues[1]
    }

    /**
     * Extracts the reminder/note payload: text after a trigger phrase like
     * "remind me to", "note that", "remember that".
     */
    fun extractPayload(text: String, triggers: List<String>): String? {
        val lower = text.lowercase(Locale.US)
        for (trigger in triggers) {
            val idx = lower.indexOf(trigger)
            if (idx >= 0) {
                val payload = text.substring(idx + trigger.length).trim()
                    .trimStart(':', '-', '—')
                    .trim()
                if (payload.isNotEmpty()) return payload
            }
        }
        return null
    }

    /** True if [text] parses as a relative duration ("in 5 minutes"). */
    fun hasRelativeDuration(text: String): Boolean =
        DurationParser.parseRelative(text, System.currentTimeMillis()) != null
}
