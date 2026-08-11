package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memories WHERE userId = :userId OR userId = '' ORDER BY updatedAt DESC")
    fun getAllMemories(userId: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memories WHERE (userId = :userId OR userId = '') AND enabled = 1 ORDER BY updatedAt DESC")
    suspend fun getEnabledMemories(userId: String): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE id = :id LIMIT 1")
    suspend fun getMemoryById(id: String): MemoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Query("DELETE FROM memories WHERE id = :id")
    suspend fun deleteMemoryById(id: String)

    @Query("DELETE FROM memories")
    suspend fun clearAllMemories()

    @Query("UPDATE memories SET enabled = :enabled WHERE id = :id")
    suspend fun setMemoryEnabled(id: String, enabled: Boolean)
}
