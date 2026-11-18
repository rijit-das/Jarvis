package com.jarvis.core.di

import android.content.Context
import androidx.room.Room
import com.jarvis.core.security.KeystoreManager
import com.jarvis.data.datastore.SecureSettingsRepository
import com.jarvis.data.local.AuditLogDao
import com.jarvis.data.local.ConversationDao
import com.jarvis.data.local.JarvisDatabase
import com.jarvis.data.local.MemoryDao
import com.jarvis.data.local.NoteDao
import com.jarvis.data.local.ReminderDao
import com.jarvis.data.local.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import javax.inject.Singleton

/**
 * Provides the SQLCipher-encrypted Room database. The passphrase is derived
 * from a random secret held in EncryptedSharedPreferences (Keystore-backed),
 * so the database is encrypted at rest and the key never appears in plaintext.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keystoreManager: KeystoreManager,
        secureSettings: SecureSettingsRepository
    ): JarvisDatabase {
        val passphrase = keystoreManager.databasePassphrase(
            secretStore = { secureSettings.getDbSecret() },
            persist = { secureSettings.setDbSecret(it) }
        )
        val factory = SupportFactory(SQLiteDatabase.getBytes(passphrase))
        passphrase.fill('\u0000')
        return Room.databaseBuilder(context, JarvisDatabase::class.java, "jarvis.db")
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideMemoryDao(db: JarvisDatabase): MemoryDao = db.memoryDao()

    @Provides
    fun provideNoteDao(db: JarvisDatabase): NoteDao = db.noteDao()

    @Provides
    fun provideTaskDao(db: JarvisDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideReminderDao(db: JarvisDatabase): ReminderDao = db.reminderDao()

    @Provides
    fun provideConversationDao(db: JarvisDatabase): ConversationDao = db.conversationDao()

    @Provides
    fun provideAuditLogDao(db: JarvisDatabase): AuditLogDao = db.auditLogDao()
}
