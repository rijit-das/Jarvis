package com.jarvis.tools.impl

import com.jarvis.data.local.TaskDao
import com.jarvis.data.local.TaskEntity
import com.jarvis.tools.Tool
import com.jarvis.tools.ToolResult
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Adds a task. */
@Singleton
class AddTaskTool @Inject constructor(
    private val taskDao: TaskDao
) : Tool {
    override val name = "add_task"
    override val description = "add a task"
    override val inputSchema = mapOf("title" to "Task title")
    override val outputDescription = "Confirmation"
    override val requiredPermission: String? = null
    override val requiresConfirmation = false
    override val timeoutMs = 10_000L

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val title = arguments["title"]?.trim().orEmpty()
        if (title.isEmpty()) {
            return ToolResult.Failure(spoken = "What task should I add?", reason = "missing title")
        }
        taskDao.insert(TaskEntity(title = title, createdAtMillis = System.currentTimeMillis()))
        return ToolResult.Success(spoken = "Task added: $title.")
    }
}

/** Lists open tasks. */
@Singleton
class ListTasksTool @Inject constructor(
    private val taskDao: TaskDao
) : Tool {
    override val name = "list_tasks"
    override val description = "list tasks"
    override val inputSchema = emptyMap<String, String>()
    override val outputDescription = "Spoken list of open tasks"
    override val requiredPermission: String? = null
    override val requiresConfirmation = false
    override val timeoutMs = 10_000L

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val tasks = taskDao.observeAll().first().filter { !it.done }
        if (tasks.isEmpty()) {
            return ToolResult.Success(spoken = "You have no open tasks.")
        }
        val spoken = tasks.take(5).joinToString(". ") { it.title }
        return ToolResult.Success(spoken = "Your open tasks: $spoken.")
    }
}

/** Marks a task complete by title match. */
@Singleton
class CompleteTaskTool @Inject constructor(
    private val taskDao: TaskDao
) : Tool {
    override val name = "complete_task"
    override val description = "mark a task complete"
    override val inputSchema = mapOf("title" to "Task title (partial match allowed)")
    override val outputDescription = "Confirmation"
    override val requiredPermission: String? = null
    override val requiresConfirmation = false
    override val timeoutMs = 10_000L

    override suspend fun execute(arguments: Map<String, String>): ToolResult {
        val title = arguments["title"]?.trim().orEmpty()
        if (title.isEmpty()) {
            return ToolResult.Failure(spoken = "Which task should I complete?", reason = "missing title")
        }
        val tasks = taskDao.observeAll().first().filter { !it.done }
        val match = tasks.firstOrNull { it.title.contains(title, ignoreCase = true) }
            ?: return ToolResult.Failure(
                spoken = "I couldn't find an open task matching '$title'.",
                reason = "task not found"
            )
        taskDao.setDone(match.id, true)
        return ToolResult.Success(spoken = "Task completed: ${match.title}.")
    }
}
