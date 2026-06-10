package com.mcis.memoir.data.memory

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Upsert
    suspend fun upsert(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM memories WHERE id = :id")
    suspend fun getOnce(id: String): MemoryEntity?

    @Query("SELECT * FROM memories WHERE id = :id")
    fun observe(id: String): Flow<MemoryEntity?>

    @Query("SELECT * FROM memories ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE status = :status ORDER BY updatedAt DESC")
    fun observeByStatus(status: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE status = 'IN_PROGRESS' AND updatedAt < :cutoff")
    suspend fun findStaleInProgress(cutoff: Long): List<MemoryEntity>
}
