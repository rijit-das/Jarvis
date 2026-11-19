package com.jarvis.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Memory categories. Sensitive data is never stored by default. */
enum class MemoryCategory { PREFERENCE, FACT, SUMMARY, NOTE, TASK, REMINDER }

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val key: String,
    val value: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val body: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val done: Boolean = false,
    val createdAtMillis: Long
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val triggerAtMillis: Long,
    val delivered: Boolean = false
)

@Entity(tableName = "conversation_turns")
data class ConversationTurnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String, // "user" | "assistant"
    val text: String,
    val timestampMillis: Long
)

@Entity(tableName = "audit_log")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMillis: Long,
    val eventType: String,
    val detail: String
)
