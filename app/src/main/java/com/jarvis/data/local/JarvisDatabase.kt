package com.jarvis.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Encrypted Room database (SQLCipher). The passphrase is derived from a
 * Keystore-protected secret — see `DatabaseModule`.
 */
@Database(
    entities = [
        MemoryEntity::class,
        NoteEntity::class,
        TaskEntity::class,
        ReminderEntity::class,
        ConversationTurnEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class JarvisDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
    abstract fun reminderDao(): ReminderDao
    abstract fun conversationDao(): ConversationDao
    abstract fun auditLogDao(): AuditLogDao
}
