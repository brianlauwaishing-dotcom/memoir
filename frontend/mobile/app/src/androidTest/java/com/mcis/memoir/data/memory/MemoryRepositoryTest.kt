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
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class MemoryRepositoryTest {
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
        filesDir = Files.createTempDirectory("memoir-test").toFile()
        repo = RoomMemoryRepository(dao, filesDir, json, dispatcher)
    }

    @After
    fun tearDown() {
        database.close()
        filesDir.deleteRecursively()
    }

    @Test
    fun startDraftCreatesParseableInProgressRow() = runTest(dispatcher) {
        val id = repo.startDraft("old_street", "Old Street Journal")

        assertNotNull(UUID.fromString(id))
        val row = dao.getOnce(id)
        assertNotNull(row)
        assertEquals(MemoryStatus.IN_PROGRESS, row?.status)
        assertEquals("[]", row?.photoLocalPaths)
        assertEquals("{}", row?.spotNotes)
    }

    @Test
    fun addAndRemovePhotoUpdatesFileAndJson() = runTest(dispatcher) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val id = repo.startDraft("old_street", "Old Street Journal")
        val source = File(filesDir, "source.jpg").apply { writeBytes(ByteArray(100) { 7 }) }

        val relativePath = repo.addPhoto(id, android.net.Uri.fromFile(source), context.contentResolver).getOrThrow()

        val photo = File(filesDir, relativePath)
        assertTrue(photo.exists())
        assertEquals(100L, photo.length())
        assertEquals(listOf(relativePath), json.decodeFromString<List<String>>(dao.getOnce(id)!!.photoLocalPaths))

        repo.removePhoto(id, 0).getOrThrow()
        assertFalse(photo.exists())
        assertEquals(emptyList<String>(), json.decodeFromString<List<String>>(dao.getOnce(id)!!.photoLocalPaths))
    }

    @Test
    fun invalidAddPhotoReturnsFailureWithoutWriting() = runTest(dispatcher) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = File(filesDir, "source.jpg").apply { writeBytes(byteArrayOf(1)) }

        val result = repo.addPhoto("../etc/passwd", android.net.Uri.fromFile(source), context.contentResolver)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertFalse(File(filesDir, "memories").exists())
    }

    @Test
    fun cancelDraftDeletesOnlyInProgressRows() = runTest(dispatcher) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val draftId = repo.startDraft("old_street", "Old Street Journal")
        val source = File(filesDir, "source.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        repo.addPhoto(draftId, android.net.Uri.fromFile(source), context.contentResolver).getOrThrow()

        repo.cancelDraftIfInProgress(draftId)

        assertEquals(null, dao.getOnce(draftId))
        assertFalse(File(filesDir, "memories/$draftId").exists())

        val completedId = repo.startDraft("old_street", "Old Street Journal")
        repo.addPhoto(completedId, android.net.Uri.fromFile(source), context.contentResolver).getOrThrow()
        repo.complete(completedId)
        repo.cancelDraftIfInProgress(completedId)

        assertNotNull(dao.getOnce(completedId))
        assertTrue(File(filesDir, "memories/$completedId").exists())
        repo.cancelDraftIfInProgress("not-a-uuid")
    }

    @Test
    fun sweepOrphansDeletesOldInProgressAndPreservesRecentAndCompleted() = runTest(dispatcher) {
        val now = System.currentTimeMillis()
        val oldDraft = insertRow(status = MemoryStatus.IN_PROGRESS, updatedAt = now - 8L * 24L * 60L * 60L * 1000L)
        val recentDraft = insertRow(status = MemoryStatus.IN_PROGRESS, updatedAt = now - 60L * 60L * 1000L)
        val completed = insertRow(status = MemoryStatus.COMPLETED, updatedAt = now - 30L * 24L * 60L * 60L * 1000L)

        repo.sweepOrphans()

        assertEquals(null, dao.getOnce(oldDraft))
        assertFalse(File(filesDir, "memories/$oldDraft").exists())
        assertNotNull(dao.getOnce(recentDraft))
        assertNotNull(dao.getOnce(completed))
    }

    private suspend fun insertRow(status: String, updatedAt: Long): String {
        val id = UUID.randomUUID().toString()
        File(filesDir, "memories/$id").mkdirs()
        dao.upsert(
            MemoryEntity(
                id = id,
                templateId = "old_street",
                title = "Old Street Journal",
                status = status,
                createdAt = updatedAt,
                updatedAt = updatedAt,
                photoLocalPaths = "[]",
                spotNotes = "{}"
            )
        )
        return id
    }
}
