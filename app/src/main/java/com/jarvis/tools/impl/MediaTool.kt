package com.jarvis.tools.impl

import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Build
import com.jarvis.tools.Tool
import com.jarvis.tools.ToolResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Controls the active media session (play/pause/next/previous) via
 * MediaSessionManager. If no session is active, reports that honestly.
 */
@Singleton
class MediaTool @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun activeController(): MediaController? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return null
        val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            ?: return null
        return try {
            manager.activeSessions.firstOrNull()
        } catch (_: SecurityException) {
            null
        }
    }

    class PlayTool @Inject constructor(private val mediaTool: MediaTool) : Tool {
        override val name = "media_play"
        override val description = "play media"
        override val inputSchema = emptyMap<String, String>()
        override val outputDescription = "Confirmation"
        override val requiredPermission: String? = null
        override val requiresConfirmation = false
        override val timeoutMs = 5_000L

        override suspend fun execute(arguments: Map<String, String>): ToolResult {
            val controller = mediaTool.activeController()
                ?: return ToolResult.Failure(
                    spoken = "No media is currently active. Start music in another app first.",
                    reason = "no active media session"
                )
            controller.transportControls.play()
            return ToolResult.Success(spoken = "Playing.")
        }
    }

    class PauseTool @Inject constructor(private val mediaTool: MediaTool) : Tool {
        override val name = "media_pause"
        override val description = "pause media"
        override val inputSchema = emptyMap<String, String>()
        override val outputDescription = "Confirmation"
        override val requiredPermission: String? = null
        override val requiresConfirmation = false
        override val timeoutMs = 5_000L

        override suspend fun execute(arguments: Map<String, String>): ToolResult {
            val controller = mediaTool.activeController()
                ?: return ToolResult.Failure(
                    spoken = "No media is currently active.",
                    reason = "no active media session"
                )
            controller.transportControls.pause()
            return ToolResult.Success(spoken = "Paused.")
        }
    }

    class NextTool @Inject constructor(private val mediaTool: MediaTool) : Tool {
        override val name = "media_next"
        override val description = "skip to next track"
        override val inputSchema = emptyMap<String, String>()
        override val outputDescription = "Confirmation"
        override val requiredPermission: String? = null
        override val requiresConfirmation = false
        override val timeoutMs = 5_000L

        override suspend fun execute(arguments: Map<String, String>): ToolResult {
            val controller = mediaTool.activeController()
                ?: return ToolResult.Failure(
                    spoken = "No media is currently active.",
                    reason = "no active media session"
                )
            controller.transportControls.skipToNext()
            return ToolResult.Success(spoken = "Next track.")
        }
    }

    class PreviousTool @Inject constructor(private val mediaTool: MediaTool) : Tool {
        override val name = "media_previous"
        override val description = "go to previous track"
        override val inputSchema = emptyMap<String, String>()
        override val outputDescription = "Confirmation"
        override val requiredPermission: String? = null
        override val requiresConfirmation = false
        override val timeoutMs = 5_000L

        override suspend fun execute(arguments: Map<String, String>): ToolResult {
            val controller = mediaTool.activeController()
                ?: return ToolResult.Failure(
                    spoken = "No media is currently active.",
                    reason = "no active media session"
                )
            controller.transportControls.skipToPrevious()
            return ToolResult.Success(spoken = "Previous track.")
        }
    }
}
