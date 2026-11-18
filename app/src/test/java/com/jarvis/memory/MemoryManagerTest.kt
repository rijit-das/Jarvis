package com.jarvis.memory

import com.jarvis.core.common.AppResult
import com.jarvis.data.local.MemoryDao
import com.jarvis.data.local.MemoryEntity
import com.jarvis.data.repository.MemoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** In-memory fake DAO for testing the memory stack without Room. */
private class FakeMemoryDao : MemoryDao {
    private val store = mutableListOf<MemoryEntity>()
    private var nextId = 1L
    private val flow = MutableStateFlow<List<MemoryEntity>>(emptyList())

    override fun observeAll(): Flow<List<MemoryEntity>> = flow

    override fun observeByCategory(category: String): Flow<List<MemoryEntity>> =
        MutableStateFlow(store.filter { it.category == category })

    override suspend fun findByKey(key: String): MemoryEntity? =
        store.firstOrNull { it.key == key }

    override suspend fun search(query: String): List<MemoryEntity> =
        store.filter { it.key.contains(query) || it.value.contains(query) }

    override suspend fun insert(entity: MemoryEntity): Long {
        val withId = entity.copy(id = nextId++)
        store.add(withId)
        flow.value = store.toList()
        return withId.id
    }

    override suspend fun update(entity: MemoryEntity) {
        val index = store.indexOfFirst { it.id == entity.id }
        if (index >= 0) {
            store[index] = entity
            flow.value = store.toList()
        }
    }

    override suspend fun deleteById(id: Long) {
        store.removeAll { it.id == id }
        flow.value = store.toList()
    }

    override suspend fun deleteAll() {
        store.clear()
        flow.value = emptyList()
    }
}

class MemoryManagerTest {

    private fun manager(): MemoryManager =
        MemoryManager(MemoryRepository(FakeMemoryDao()))

    @Test
    fun `save and retrieve round-trip`() = runTest {
        val manager = manager()
        val result = manager.save("FACT", "birthday", "June 3")
        assertTrue(result is AppResult.Success)
        val retrieved = manager.retrieve("birthday")
        assertEquals(1, retrieved.size)
        assertEquals("June 3", retrieved[0].value)
    }

    @Test
    fun `blank key or value is rejected`() = runTest {
        val manager = manager()
        assertTrue(manager.save("FACT", "", "value") is AppResult.Failure)
        assertTrue(manager.save("FACT", "key", "") is AppResult.Failure)
    }

    @Test
    fun `upsert updates existing key`() = runTest {
        val manager = manager()
        manager.save("FACT", "color", "blue")
        manager.save("FACT", "color", "green")
        val retrieved = manager.retrieve("color")
        assertEquals(1, retrieved.size)
        assertEquals("green", retrieved[0].value)
    }

    @Test
    fun `delete removes memory`() = runTest {
        val manager = manager()
        manager.save("FACT", "temp", "value")
        val id = manager.retrieve("temp")[0].id
        manager.delete(id)
        assertTrue(manager.retrieve("temp").isEmpty())
    }

    @Test
    fun `export produces JSON`() = runTest {
        val manager = manager()
        manager.save("FACT", "a", "1")
        val json = manager.exportJson()
        assertTrue(json.contains("\"key\":\"a\""))
        assertTrue(json.contains("\"value\":\"1\""))
    }
}
