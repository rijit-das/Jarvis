package com.jarvis.tools.impl

import com.jarvis.data.local.NoteDao
import com.jarvis.data.local.NoteEntity
import com.jarvis.tools.Tool
import com.jarvis.tools.ToolResult
import javax.inject.Inject
import javax.inject.Singleton

/** Creates a note in the encrypted local DB. */
@Singleton
class CreateNoteTool @Inject constructor(
    private val noteDao: NoteDao
) : Tool {
    override val name = "create_note"
    override val description = "create a note"
    override val inputSchema = mapOf("body" to "Note text")
    override val outputDescription = "Confirmation"
    override val requiredPermission: String? = null
    override val requiresConfirmation = false
    override val timeoutMs = 10_000L

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val body = arguments["body"]?.trim().orEmpty()
        if (body.isEmpty()) {
            return ToolResult.Failure(
                spoken = "What should the note say?",
                reason = "missing note body"
            )
        }
        val now = System.currentTimeMillis()
        noteDao.insert(
            NoteEntity(
                title = body.take(40),
                body = body,
                createdAtMillis = now,
                updatedAtMillis = now
            )
        )
        return ToolResult.Success(spoken = "Note saved.")
    }
}

/** Lists recent notes. */
@Singleton
class ListNotesTool @Inject constructor(
    private val noteDao: NoteDao
) : Tool {
    override val name = "list_notes"
    override val description = "list recent notes"
    override val inputSchema = emptyMap<String, String>()
    override val outputDescription = "Spoken list of recent notes"
    override val requiredPermission: String? = null
    override val requiresConfirmation = false
    override val timeoutMs = 10_000L

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        // Empty search matches all rows (LIKE '%%').
        val result = noteDao.search("")
        if (result.isEmpty()) {
            return ToolResult.Success(spoken = "You have no notes yet.")
        }
        val spoken = result.take(5).joinToString(". ") { it.body.take(80) }
        return ToolResult.Success(spoken = "Your recent notes: $spoken.")
    }
}

/** Searches notes by keyword. */
@Singleton
class SearchNotesTool @Inject constructor(
    private val noteDao: NoteDao
) : Tool {
    override val name = "search_notes"
    override val description = "search notes"
    override val inputSchema = mapOf("query" to "Search keywords")
    override val outputDescription = "Spoken matching notes"
    override val requiredPermission: String? = null
    override val requiresConfirmation = false
    override val timeoutMs = 10_000L

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val query = arguments["query"]?.trim().orEmpty()
        if (query.isEmpty()) {
            return ToolResult.Failure(spoken = "What should I search for?", reason = "missing query")
        }
        val matches = noteDao.search(query)
        if (matches.isEmpty()) {
            return ToolResult.Success(spoken = "No notes match '$query'.")
        }
        val spoken = matches.take(3).joinToString(". ") { it.body.take(100) }
        return ToolResult.Success(spoken = "Found ${matches.size} note(s): $spoken.")
    }
}
