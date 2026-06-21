package com.mcis.memoir.data.memory

import android.content.ContentResolver
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RoomMemoryRepository(
    private val dao: MemoryDao,
    private val filesDir: File,
    private val json: Json,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : MemoryRepository {
    private val cleanupScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val uuidRegex = Regex("^[0-9a-fA-F\\-]{36}$")

    override suspend fun startDraft(templateId: String, defaultTitle: String): String = withContext(ioDispatcher) {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        dao.upsert(
            MemoryEntity(
                id = id,
                templateId = templateId,
                routeId = null,
                title = defaultTitle,
                status = MemoryStatus.IN_PROGRESS,
                createdAt = now,
                updatedAt = now,
                photoLocalPaths = "[]",
                spotNotes = "{}"
            )
        )
        id
    }

    override fun observe(id: String): Flow<Memory?> = dao.observe(id).map { it?.toDomain() }

    override fun observeAll(): Flow<List<Memory>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeByStatus(status: String): Flow<List<Memory>> =
        dao.observeByStatus(status).map { rows -> rows.map { it.toDomain() } }

    override suspend fun addPhoto(
        memoryId: String,
        sourceUri: Uri,
        contentResolver: ContentResolver
    ): Result<String> = withContext(ioDispatcher) {
        runCatching {
            require(memoryId.isValidUuid()) { "memoryId must be a UUID" }
            val current = dao.getOnce(memoryId) ?: error("memory not found: $memoryId")
            val currentPaths = current.photoPaths()
            val index = currentPaths.size
            val relativePath = "memories/$memoryId/photo_$index.jpg"
            val dest = File(filesDir, relativePath)
            dest.parentFile?.mkdirs()

            contentResolver.openInputStream(sourceUri).use { input ->
                FileOutputStream(dest).use { output ->
                    input?.copyTo(output) ?: error("openInputStream returned null")
                }
            }

            dao.upsert(
                current.copy(
                    photoLocalPaths = json.encodeToString(currentPaths + relativePath),
                    updatedAt = System.currentTimeMillis()
                )
            )
            relativePath
        }
    }

    override suspend fun removePhoto(memoryId: String, index: Int): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            require(memoryId.isValidUuid()) { "memoryId must be a UUID" }
            val current = dao.getOnce(memoryId) ?: error("memory not found: $memoryId")
            val currentPaths = current.photoPaths()
            require(index in currentPaths.indices) { "photo index out of bounds" }

            val removedPath = currentPaths[index]
            val target = File(filesDir, removedPath)
            if (target.isUnder(memoriesRoot())) {
                target.delete()
            }

            dao.upsert(
                current.copy(
                    photoLocalPaths = json.encodeToString(currentPaths.filterIndexed { i, _ -> i != index }),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    override suspend fun updateReflection(
        memoryId: String,
        mood: String?,
        insights: String,
        feedback: String?
    ) = withContext(ioDispatcher) {
        val current = dao.getOnce(memoryId) ?: error("memory not found: $memoryId")
        dao.upsert(
            current.copy(
                overallMood = mood,
                userInsights = insights,
                postTripFeedback = feedback,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun updateGeneratedReflection(memoryId: String, text: String) = withContext(ioDispatcher) {
        val row = dao.getOnce(memoryId) ?: return@withContext
        dao.upsert(
            row.copy(
                generatedReflection = text,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun complete(memoryId: String) = withContext(ioDispatcher) {
        val current = dao.getOnce(memoryId) ?: error("memory not found: $memoryId")
        dao.upsert(
            current.copy(
                status = MemoryStatus.COMPLETED,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    override suspend fun cancelDraftIfInProgress(memoryId: String) = withContext(ioDispatcher) {
        if (!memoryId.isValidUuid()) return@withContext
        val row = dao.getOnce(memoryId) ?: return@withContext
        if (row.status != MemoryStatus.IN_PROGRESS) return@withContext
        deleteMemoryDirectory(memoryId)
        dao.delete(memoryId)
    }

    override fun fireCancelDraftIfInProgress(memoryId: String) {
        cleanupScope.launch { cancelDraftIfInProgress(memoryId) }
    }

    override suspend fun deleteMemory(memoryId: String) = withContext(ioDispatcher) {
        if (!memoryId.isValidUuid()) return@withContext
        deleteMemoryDirectory(memoryId)
        dao.delete(memoryId)
    }

    override suspend fun duplicateMemory(memoryId: String): Result<String> = withContext(ioDispatcher) {
        var allocatedNewId: String? = null
        runCatching {
            require(memoryId.isValidUuid()) { "memoryId must be a UUID" }
            val source = dao.getOnce(memoryId) ?: error("memory not found: $memoryId")
            val newId = UUID.randomUUID().toString()
            allocatedNewId = newId
            val sourcePaths = source.photoPaths()
            val copiedPaths = sourcePaths.mapIndexed { index, sourcePath ->
                val sourceFile = File(filesDir, sourcePath)
                val destPath = "memories/$newId/photo_$index.jpg"
                val destFile = File(filesDir, destPath)
                destFile.parentFile?.mkdirs()
                sourceFile.copyTo(destFile, overwrite = true)
                destPath
            }
            val now = System.currentTimeMillis()
            dao.upsert(
                source.copy(
                    id = newId,
                    createdAt = now,
                    updatedAt = now,
                    photoLocalPaths = json.encodeToString(copiedPaths)
                )
            )
            newId
        }.onFailure {
            allocatedNewId?.let { deleteMemoryDirectory(it) }
        }
    }

    override suspend fun sweepOrphans() = withContext(ioDispatcher) {
        val cutoff = System.currentTimeMillis() - SEVEN_DAYS_MS
        dao.findStaleInProgress(cutoff).forEach { row ->
            deleteMemoryDirectory(row.id)
            dao.delete(row.id)
        }
    }

    private fun MemoryEntity.toDomain(): Memory = Memory(
        id = id,
        templateId = templateId,
        routeId = routeId,
        title = title,
        status = status,
        createdAt = createdAt,
        updatedAt = updatedAt,
        photoRelativePaths = photoPaths(),
        spotNotes = json.decodeFromString(spotNotes),
        overallMood = overallMood,
        userInsights = userInsights,
        postTripFeedback = postTripFeedback,
        generatedReflection = generatedReflection,
        editorState = editorState
    )

    private fun MemoryEntity.photoPaths(): List<String> = json.decodeFromString(photoLocalPaths)

    private fun String.isValidUuid(): Boolean = matches(uuidRegex)

    private fun deleteMemoryDirectory(memoryId: String) {
        val dir = File(filesDir, "memories/$memoryId")
        if (dir.exists() && dir.isUnder(memoriesRoot())) {
            dir.deleteRecursively()
        }
    }

    private fun memoriesRoot(): File = File(filesDir, "memories")

    private fun File.isUnder(parent: File): Boolean {
        // File.toPath() is API 26+; minSdk is 24, so compare canonical path strings instead.
        // Match Path.startsWith semantics (component boundary) by anchoring on the separator.
        val targetPath = canonicalFile.path
        val parentPath = parent.canonicalFile.path
        return targetPath == parentPath || targetPath.startsWith(parentPath + File.separator)
    }

    private companion object {
        const val SEVEN_DAYS_MS = 7L * 24L * 60L * 60L * 1000L
    }
}
