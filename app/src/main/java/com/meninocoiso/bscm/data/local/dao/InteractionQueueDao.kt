package com.meninocoiso.bscm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.meninocoiso.bscm.data.local.entity.QueuedInteractionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InteractionQueueDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteraction(interaction: QueuedInteractionEntity)
    
    @Query("SELECT * FROM interaction_queue ORDER BY timestamp ASC")
    suspend fun getAllQueuedInteractions(): List<QueuedInteractionEntity>
    
    @Query("SELECT * FROM interaction_queue ORDER BY timestamp ASC")
    fun getAllQueuedInteractionsFlow(): Flow<List<QueuedInteractionEntity>>
    
    @Query("DELETE FROM interaction_queue WHERE id = :id")
    suspend fun deleteInteraction(id: String)
    
    @Update
    suspend fun updateInteraction(interaction: QueuedInteractionEntity)
    
    @Query("SELECT COUNT(*) FROM interaction_queue")
    suspend fun getQueueSize(): Int
    
    @Query("DELETE FROM interaction_queue WHERE retryCount >= maxRetries")
    suspend fun deleteFailedInteractions()
}
