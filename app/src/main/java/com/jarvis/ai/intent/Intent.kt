package com.jarvis.ai.intent

/**
 * Intent taxonomy for the offline intent engine. Every intent maps to either
 * a tool call, a direct answer, or a clarification request.
 */
enum class IntentType {
    TIME_NOW,
    DATE_NOW,
    SET_TIMER,
    SET_REMINDER,
    CREATE_NOTE,
    LIST_NOTES,
    SEARCH_NOTES,
    ADD_TASK,
    LIST_TASKS,
    COMPLETE_TASK,
    OPEN_APP,
    PLAY_MEDIA,
    PAUSE_MEDIA,
    NEXT_TRACK,
    PREVIOUS_TRACK,
    NAVIGATE,
    READ_NOTIFICATIONS,
    REMEMBER,
    RECALL,
    FORGET,
    REPEAT_LAST,
    HELP,
    GREETING,
    FAREWELL,
    THANK_YOU,
    UNKNOWN
}

/**
 * Classified intent with extracted entities.
 *
 * @param rawText original user text
 * @param entities named slots (e.g. "app" -> "camera", "duration" -> "5 minutes")
 * @param confidence 0..1 heuristic confidence
 */
data class Intent(
    val type: IntentType,
    val rawText: String,
    val entities: Map<String, String> = emptyMap(),
    val confidence: Float = 1f
)
