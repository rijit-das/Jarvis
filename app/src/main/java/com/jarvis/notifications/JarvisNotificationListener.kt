package com.jarvis.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Optional notification listener. Only active if the user manually enables
 * JARVIS in system notification-access settings. Keeps a small in-memory
 * ring buffer of recent notifications (never persisted).
 */
class JarvisNotificationListener : NotificationListenerService() {

    data class RecentNotification(val app: String, val text: String, val timestampMillis: Long)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val text = extras.getCharSequence("android.text")?.toString()
            ?: extras.getCharSequence("android.title")?.toString()
            ?: return
        synchronized(buffer) {
            buffer.add(RecentNotification(sbn.packageName, text, System.currentTimeMillis()))
            while (buffer.size > MAX_BUFFER) buffer.removeAt(0)
        }
    }

    companion object {
        private const val MAX_BUFFER = 20
        private val buffer = ArrayDeque<RecentNotification>()

        fun recentNotifications(): List<RecentNotification> =
            synchronized(buffer) { buffer.toList().reversed() }
    }
}
