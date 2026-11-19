package com.jarvis.tools.impl

import com.jarvis.tools.Tool
import com.jarvis.tools.ToolResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Reads the current time / date. No permission required. */
@Singleton
class TimeTool @Inject constructor() : Tool {
    override val name = "time_now"
    override val description = "read the current time"
    override val inputSchema = emptyMap<String, String>()
    override val outputDescription = "Spoken current time"
    override val requiredPermission: String? = null
    override val requiresConfirmation = false
    override val timeoutMs = 5_000L

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val format = SimpleDateFormat("h:mm a", Locale.US)
        val time = format.format(Date())
        return ToolResult.Success(spoken = "It's $time.")
    }
}

/** Reads the current date. No permission required. */
@Singleton
class DateTool @Inject constructor() : Tool {
    override val name = "date_now"
    override val description = "read the current date"
    override val inputSchema = emptyMap<String, String>()
    override val outputDescription = "Spoken current date"
    override val requiredPermission: String? = null
    override val requiresConfirmation = false
    override val timeoutMs = 5_000L

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val format = SimpleDateFormat("EEEE, MMMM d", Locale.US)
        val date = format.format(Date())
        return ToolResult.Success(spoken = "Today is $date.")
    }
}
