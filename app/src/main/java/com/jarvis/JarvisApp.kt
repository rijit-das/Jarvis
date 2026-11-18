package com.jarvis

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.jarvis.core.network.ConnectivityMonitor
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class JarvisApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var connectivityMonitor: ConnectivityMonitor

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        connectivityMonitor.register()
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
}
