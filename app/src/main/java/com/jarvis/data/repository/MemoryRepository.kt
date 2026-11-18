package com.jarvis.data.repository

import com.jarvis.data.local.MemoryDao
import com.jarvis.data.local.MemoryEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Repository facade over [MemoryDao]. */
@Singleton
class MemoryRepository @Inject constructor(
    private val dao: MemoryDao
) {
    fun observeAll(): Flow<List<MemoryEntity>> = dao.observeAll()

    fun observeByCategory(category: String): Flow<List<MemoryEntity>> =
        dao.observeByCategory(category)

    suspend fun findByKey(key: String): MemoryEntity? = dao.findByKey(key)

    suspend fun search(query: String): List<MemoryEntity> = dao.search(query)

    suspend fun upsert(category: String, key: String, value: String): Long {
        val now = System.currentTimeMillis()
        val existing = dao.findByKey(key)
        return if (existing != null) {
            dao.update(existing.copy(value = value, updatedAtMillis = now))
            existing.id
        } else {
            dao.insert(
                MemoryEntity(
                    category = category,
                    key = key,
                    value = value,
                    createdAtMillis = now,
                    updatedAtMillis = now
                )
            )
        }
    }

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun deleteAll() = dao.deleteAll()
}
