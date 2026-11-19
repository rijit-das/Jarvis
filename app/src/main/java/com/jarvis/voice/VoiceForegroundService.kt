package com.jarvis.voice

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jarvis.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service for the voice session (microphone type). Shows a
 * persistent notification with a stop action so the user always knows the
 * mic is in use and can stop it immediately.
 */
@AndroidEntryPoint
class VoiceForegroundService : Service() {

    @Inject
    lateinit var voiceSessionManager: VoiceSessionManager

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startForegroundServiceWithNotification()
            ACTION_STOP -> {
                voiceSessionManager.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        val notification: Notification = NotificationCompat.Builder(this, "voice_session")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle("JARVIS is listening")
            .setContentText("Tap stop to end the voice session.")
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(1, notification)
        }
    }

    companion object {
        const val ACTION_START = "com.jarvis.action.VOICE_START"
        const val ACTION_STOP = "com.jarvis.action.VOICE_STOP"
    }
}
