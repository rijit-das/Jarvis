package com.jarvis.ai.intent

import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deterministic, offline, pattern-based intent classifier.
 *
 * This is the v1 "brain": fast, predictable, fully offline, and honest about
 * its limits (returns [IntentType.UNKNOWN] rather than guessing). It is
 * locale-parameterised so additional languages can be added as pattern packs
 * without changing the pipeline.
 */
@Singleton
class IntentEngine @Inject constructor() {

    fun classify(rawText: String): Intent {
        val text = rawText.trim()
        if (text.isEmpty()) return Intent(IntentType.UNKNOWN, rawText, confidence = 0f)
        val lower = text.lowercase(Locale.US)

        // --- Greetings / social ---
        if (lower.matches(Regex("""(hi|hello|hey|hey jarvis|good (morning|afternoon|evening)|yo)\b.*"""))) {
            return Intent(IntentType.GREETING, text)
        }
        if (lower.contains("thank")) return Intent(IntentType.THANK_YOU, text)
        if (lower.matches(Regex("""(bye|goodbye|good night|see you).*)"""))) {
            return Intent(IntentType.FAREWELL, text)
        }
        if (lower.contains("what can you do") || lower == "help" || lower.startsWith("help ")) {
            return Intent(IntentType.HELP, text)
        }

        // --- Time / date ---
        if (lower.contains("what time") || lower == "time" || lower.contains("current time")) {
            return Intent(IntentType.TIME_NOW, text)
        }
        if (lower.contains("what day") || lower.contains("what date") || lower.contains("today's date")) {
            return Intent(IntentType.DATE_NOW, text)
        }

        // --- Reminders & timers ---
        if (lower.startsWith("remind me") || lower.contains("remind me to")) {
            val entities = mutableMapOf<String, String>()
            EntityExtractor.extractPayload(text, listOf("remind me to", "remind me"))?.let {
                entities["message"] = it
            }
            EntityExtractor.extractDuration(text)?.let { entities["duration"] = it }
            EntityExtractor.extractClockTime(text)?.let { entities["time"] = it }
            return Intent(IntentType.SET_REMINDER, text, entities)
        }
        if (lower.startsWith("set a timer") || lower.startsWith("timer") || lower.contains("set timer")) {
            val entities = mutableMapOf<String, String>()
            EntityExtractor.extractDuration(text)?.let { entities["duration"] = it }
            return Intent(IntentType.SET_TIMER, text, entities)
        }

        // --- Notes ---
        if (lower.startsWith("note that") || lower.startsWith("take a note") ||
            lower.startsWith("write a note") || lower.startsWith("create a note")
        ) {
            val entities = mutableMapOf<String, String>()
            EntityExtractor.extractPayload(
                text, listOf("note that", "take a note", "write a note", "create a note")
            )?.let { entities["body"] = it }
            EntityExtractor.extractQuoted(text)?.let { entities["body"] = it }
            return Intent(IntentType.CREATE_NOTE, text, entities)
        }
        if (lower.contains("my notes") || lower == "list notes") {
            return Intent(IntentType.LIST_NOTES, text)
        }
        if (lower.startsWith("search notes for") || lower.startsWith("find notes about")) {
            val entities = mutableMapOf<String, String>()
            EntityExtractor.extractPayload(text, listOf("search notes for", "find notes about"))
                ?.let { entities["query"] = it }
            return Intent(IntentType.SEARCH_NOTES, text, entities)
        }

        // --- Tasks ---
        if (lower.startsWith("add task") || lower.startsWith("new task") || lower.startsWith("todo")) {
            val entities = mutableMapOf<String, String>()
            EntityExtractor.extractPayload(text, listOf("add task", "new task", "todo"))
                ?.let { entities["title"] = it }
            return Intent(IntentType.ADD_TASK, text, entities)
        }
        if (lower.contains("my tasks") || lower == "list tasks") {
            return Intent(IntentType.LIST_TASKS, text)
        }
        if (lower.startsWith("complete task") || lower.startsWith("finish task") || lower.startsWith("done task")) {
            val entities = mutableMapOf<String, String>()
            EntityExtractor.extractPayload(text, listOf("complete task", "finish task", "done task"))
                ?.let { entities["title"] = it }
            return Intent(IntentType.COMPLETE_TASK, text, entities)
        }

        // --- Memory ---
        if (lower.startsWith("remember that") || lower.startsWith("remember ")) {
            val entities = mutableMapOf<String, String>()
            EntityExtractor.extractPayload(text, listOf("remember that", "remember"))
                ?.let { entities["fact"] = it }
            return Intent(IntentType.REMEMBER, text, entities)
        }
        if (lower.startsWith("what do you remember") || lower.startsWith("recall")) {
            val entities = mutableMapOf<String, String>()
            EntityExtractor.extractPayload(text, listOf("recall", "what do you remember"))
                ?.let { entities["query"] = it }
            return Intent(IntentType.RECALL, text, entities)
        }
        if (lower.startsWith("forget")) {
            val entities = mutableMapOf<String, String>()
            EntityExtractor.extractPayload(text, listOf("forget"))?.let { entities["key"] = it }
            return Intent(IntentType.FORGET, text, entities)
        }

        // --- Apps ---
        if (lower.startsWith("open ")) {
            val entities = mutableMapOf<String, String>()
            val app = text.substring(5).trim()
            if (app.isNotEmpty()) entities["app"] = app
            return Intent(IntentType.OPEN_APP, text, entities)
        }

        // --- Media ---
        if (lower.contains("pause") || lower.contains("stop the music")) {
            return Intent(IntentType.PAUSE_MEDIA, text)
        }
        if (lower.contains("next") && (lower.contains("song") || lower.contains("track") || lower.contains("music"))) {
            return Intent(IntentType.NEXT_TRACK, text)
        }
        if (lower.contains("previous") && (lower.contains("song") || lower.contains("track") || lower.contains("music"))) {
            return Intent(IntentType.PREVIOUS_TRACK, text)
        }
        if (lower.contains("play") && (lower.contains("music") || lower.contains("song") || lower.contains("track"))) {
            return Intent(IntentType.PLAY_MEDIA, text)
        }

        // --- Navigation ---
        if (lower.startsWith("navigate to") || lower.startsWith("directions to") || lower.startsWith("take me to")) {
            val entities = mutableMapOf<String, String>()
            EntityExtractor.extractPayload(text, listOf("navigate to", "directions to", "take me to"))
                ?.let { entities["destination"] = it }
            return Intent(IntentType.NAVIGATE, text, entities)
        }

        // --- Notifications ---
        if (lower.contains("notification")) {
            return Intent(IntentType.READ_NOTIFICATIONS, text)
        }

        // --- Contextual repeat ---
        if (lower == "open that again" || lower == "do that again" || lower == "repeat") {
            return Intent(IntentType.REPEAT_LAST, text)
        }

        return Intent(IntentType.UNKNOWN, text, confidence = 0f)
    }

    /** Human-readable list of supported commands (for HELP + onboarding). */
    fun supportedCommands(): List<String> = listOf(
        "What time is it?",
        "Remind me to call Sam in 10 minutes",
        "Set a timer for 5 minutes",
        "Note that the Wi-Fi password is in the drawer",
        "Add task buy milk",
        "Open camera",
        "Play music / Pause / Next track",
        "Navigate to Central Park",
        "Remember that my birthday is June 3",
        "What do you remember about birthday?"
    )
}
