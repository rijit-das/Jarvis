package com.jarvis

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.jarvis.core.network.ConnectivityMonitor
import com.jarvis.voice.VoiceActivationManager
import com.jarvis.workers.CleanupWorker
import com.jarvis.workers.ReminderWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class JarvisApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var connectivityMonitor: ConnectivityMonitor

    @Inject
    lateinit var voiceActivationManager: VoiceActivationManager

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        connectivityMonitor.register()
        voiceActivationManager.start()
        schedulePeriodicWork()
    }

    override fun onTerminate() {
        connectivityMonitor.unregister()
        super.onTerminate()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    "voice_session",
                    getString(R.string.voice_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply { description = getString(R.string.voice_channel_desc) }
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    "reminders",
                    getString(R.string.reminder_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply { description = getString(R.string.reminder_channel_desc) }
            )
        }
    }

    private fun schedulePeriodicWork() {
        val workManager = WorkManager.getInstance(this)
        // 30-day retention cleanup for conversation turns + audit log.
        val cleanup = PeriodicWorkRequestBuilder<CleanupWorker>(30, TimeUnit.DAYS).build()
        workManager.enqueueUniquePeriodicWork(
            "jarvis_cleanup", ExistingPeriodicWorkPolicy.KEEP, cleanup
        )
        // 15-minute fallback check for overdue reminders (exact-alarm fallback).
        val reminderCheck = PeriodicWorkRequestBuilder<ReminderWorker>(15, TimeUnit.MINUTES).build()
        workManager.enqueueUniquePeriodicWork(
            "jarvis_reminder_check", ExistingPeriodicWorkPolicy.KEEP, reminderCheck
        )
    }
}
