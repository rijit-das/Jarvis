package com.jarvis.tools.impl

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.jarvis.tools.Tool
import com.jarvis.tools.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Opens an installed app via its launcher intent. Only apps resolvable through
 * PackageManager can be opened — no arbitrary intents.
 */
@Singleton
class OpenAppTool @Inject constructor(
    @ApplicationContext private val context: Context
) : Tool {
    override val name = "open_app"
    override val description = "open an app"
    override val inputSchema = mapOf("app" to "App name, e.g. 'camera'")
    override val outputDescription = "Confirmation that the app was launched"
    override val requiredPermission: String? = null
    override val requiresConfirmation = false
    override val timeoutMs = 10_000L

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val appName = arguments["app"]?.trim().orEmpty()
        if (appName.isEmpty()) {
            return ToolResult.Failure(spoken = "Which app should I open?", reason = "missing app name")
        }
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = packageManager.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
        val match = apps.firstOrNull { info ->
            val label = info.loadLabel(packageManager).toString()
            label.equals(appName, ignoreCase = true) ||
                info.activityInfo.packageName.contains(appName, ignoreCase = true)
        } ?: return ToolResult.Failure(
            spoken = "I couldn't find an app called $appName on this device.",
            reason = "app not found: $appName"
        )
        val packageName = match.activityInfo.packageName
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            ?: return ToolResult.Failure(
                spoken = "I found $appName but couldn't launch it.",
                reason = "no launch intent for $packageName"
            )
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(launchIntent)
            ToolResult.Success(spoken = "Opening $appName.")
        } catch (e: Exception) {
            ToolResult.Failure(
                spoken = "I couldn't open $appName. ${e.message ?: ""}".trim(),
                reason = e.message ?: "launch failed"
            )
        }
    }
}
