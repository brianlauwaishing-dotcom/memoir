package com.mcis.memoir.data.memory

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.nio.file.Files
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MemoryRepositoryActionsTest {
    private lateinit var database: MemoryDatabase
    private lateinit var dao: MemoryDao
    private lateinit var filesDir: File
    private lateinit var repo: RoomMemoryRepository
    private val dispatcher = StandardTestDispatcher()
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        database = Room.inMemoryDatabaseBuilder(context, MemoryDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.memoryDao()
        filesDir = Files.createTempDirectory("memoir-actions-test").toFile()
        repo = RoomMemoryRepository(dao, filesDir, json, dispatcher)
    }

    @After
    fun tearDown() {
        database.close()
        filesDir.deleteRecursively()
    }

    @Test
    fun deleteRemovesCompletedRowAndDirectory() = runTest(dispatcher) {
        val id = insertRowWithPhotos(MemoryStatus.COMPLETED, photoCount = 1)

        repo.deleteMemory(id)

        assertEquals(null, dao.getOnce(id))
        assertFalse(File(filesDir, "memories/$id").exists())
    }

    @Test
    fun deleteRemovesInProgressRowAndDirectory() = runTest(dispatcher) {
        val id = insertRowWithPhotos(MemoryStatus.IN_PROGRESS, photoCount = 1)

        repo.deleteMemory(id)

        assertEquals(null, dao.getOnce(id))
        assertFalse(File(filesDir, "memories/$id").exists())
    }

    @Test
    fun deleteWithInvalidIdIsGuardedNoOp() = runTest(dispatcher) {
        val sibling = File(filesDir, "secret").apply { mkdirs() }

        repo.deleteMemory("../secret")

        assertTrue(sibling.exists())
    }

    @Test
    fun duplicateCopiesPhotosPreservesStatusAndAssignsNewIdentity() = runTest(dispatcher) {
        val sourceId = insertRowWithPhotos(MemoryStatus.COMPLETED, photoCount = 2)
        val sourcePaths = (0 until 2).map { "memories/$sourceId/photo_$it.jpg" }

        val newId = repo.duplicateMemory(sourceId).getOrThrow()

        assertNotEquals(sourceId, newId)
        assertNotNull(UUID.fromString(newId))

        val newRow = dao.getOnce(newId)
        assertNotNull(newRow)
        assertEquals(MemoryStatus.COMPLETED, newRow?.status)

        val newPaths = json.decodeFromString<List<String>>(newRow!!.photoLocalPaths)
        assertEquals(listOf("memories/$newId/photo_0.jpg", "memories/$newId/photo_1.jpg"), newPaths)

        sourcePaths.forEachIndexed { index, sourcePath ->
            val sourceBytes = File(filesDir, sourcePath).readBytes()
            val copiedBytes = File(filesDir, newPaths[index]).readBytes()
            assertArrayEquals(sourceBytes, copiedBytes)
        }
    }

    private suspend fun insertRowWithPhotos(status: String, photoCount: Int): String {
        val id = UUID.randomUUID().toString()
        val dir = File(filesDir, "memories/$id").apply { mkdirs() }
        val paths = (0 until photoCount).map { index ->
            val relativePath = "memories/$id/photo_$index.jpg"
            File(dir, "photo_$index.jpg").writeBytes(ByteArray(50) { (index + 1).toByte() })
            relativePath
        }
        dao.upsert(
            MemoryEntity(
                id = id,
                templateId = "old_street",
                title = "Old Street Journal",
                status = status,
                createdAt = 1_000L,
                updatedAt = 1_000L,
                photoLocalPaths = json.encodeToString(paths),
                spotNotes = "{}"
            )
        )
        return id
    }
}
