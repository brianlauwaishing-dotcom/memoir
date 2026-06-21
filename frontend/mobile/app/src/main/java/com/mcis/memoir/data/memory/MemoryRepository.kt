package com.mcis.memoir.data.memory

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface MemoryRepository {
    suspend fun startDraft(templateId: String, defaultTitle: String): String
    suspend fun getOrCreateSpotDraft(spotId: String, templateId: String, defaultTitle: String): String
    fun observe(id: String): Flow<Memory?>
    fun observeAll(): Flow<List<Memory>>
    fun observeByStatus(status: String): Flow<List<Memory>>
    suspend fun addPhoto(memoryId: String, sourceUri: Uri, contentResolver: ContentResolver): Result<String>
    suspend fun removePhoto(memoryId: String, index: Int): Result<Unit>
    suspend fun updateReflection(memoryId: String, mood: String?, insights: String, feedback: String?)
    suspend fun updateGeneratedReflection(memoryId: String, text: String)
    suspend fun complete(memoryId: String)
    suspend fun cancelDraftIfInProgress(memoryId: String)
    fun fireCancelDraftIfInProgress(memoryId: String)
    suspend fun deleteMemory(memoryId: String)
    suspend fun duplicateMemory(memoryId: String): Result<String>
    suspend fun sweepOrphans()
}
