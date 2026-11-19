package com.jarvis.tools.impl

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.jarvis.tools.Tool
import com.jarvis.tools.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Navigation handoff: opens the device's maps app with a geo query.
 * JARVIS never reads location itself — the maps app handles it.
 */
@Singleton
class NavigateTool @Inject constructor(
    @ApplicationContext private val context: Context
) : Tool {
    override val name = "navigate"
    override val description = "open navigation to a destination"
    override val inputSchema = mapOf("destination" to "Destination text, e.g. 'Central Park'")
    override val outputDescription = "Confirmation that maps was opened"
    override val requiredPermission: String? = null
    override val requiresConfirmation = false
    override val timeoutMs = 10_000L

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val destination = arguments["destination"]?.trim().orEmpty()
        if (destination.isEmpty()) {
            return ToolResult.Failure(spoken = "Where should I navigate to?", reason = "missing destination")
        }
        val encoded = URLEncoder.encode(destination, "UTF-8")
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encoded")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ToolResult.Success(spoken = "Opening navigation to $destination.")
        } catch (e: Exception) {
            ToolResult.Failure(
                spoken = "I couldn't open navigation. ${e.message ?: ""}".trim(),
                reason = e.message ?: "no maps app"
            )
        }
    }
}
