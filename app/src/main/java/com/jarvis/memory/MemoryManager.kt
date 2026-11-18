package com.jarvis.memory

import com.jarvis.core.common.AppResult
import com.jarvis.data.local.MemoryEntity
import com.jarvis.data.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User-controlled memory facade. Nothing is written without explicit user
 * intent; everything can be inspected, edited, exported, or deleted.
 */
@Singleton
class MemoryManager @Inject constructor(
    private val repository: MemoryRepository
) {
    fun observeAll(): Flow<List<MemoryEntity>> = repository.observeAll()

    fun observeByCategory(category: String): Flow<List<MemoryEntity>> =
        repository.observeByCategory(category)

    suspend fun save(category: String, key: String, value: String): AppResult<Long> {
        if (key.isBlank() || value.isBlank()) {
            return AppResult.Failure("Key and value must not be blank.")
        }
        return try {
            AppResult.Success(repository.upsert(category, key.trim(), value.trim()))
        } catch (e: Exception) {
            AppResult.Failure("Couldn't save memory: ${e.message}", e)
        }
    }

    suspend fun delete(id: Long): AppResult<Unit> {
        return try {
            repository.deleteById(id)
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure("Couldn't delete memory: ${e.message}", e)
        }
    }

    suspend fun deleteAll(): AppResult<Unit> {
        return try {
            repository.deleteAll()
            AppResult.Success(Unit)
        } catch (e: Exception) {
            AppResult.Failure("Couldn't clear memory: ${e.message}", e)
        }
    }

    /** Structured retrieval: keyword search, never a full-history dump. */
    suspend fun retrieve(query: String, limit: Int = 5): List<MemoryEntity> =
        repository.search(query).take(limit)

    /** Exports all memories as JSON (for user-initiated export). */
    suspend fun exportJson(): String {
        val all = repository.search("")
        val items = all.joinToString(",\n") { entity ->
            """  {"category":"${escape(entity.category)}","key":"${escape(entity.key)}","value":"${escape(entity.value)}"}"""
        }
        return "[\n$items\n]"
    }

    private fun escape(value: String): String =
        value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
}
