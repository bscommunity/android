package com.meninocoiso.bscm.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.meninocoiso.bscm.data.local.entity.QueuedInteractionEntity

@Dao
interface InteractionQueueDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(interaction: QueuedInteractionEntity): Long
    
    @Update
    suspend fun update(interaction: QueuedInteractionEntity)
    
    @Query("SELECT * FROM interaction_queue ORDER BY timestamp ASC")
    suspend fun getAllQueued(): List<QueuedInteractionEntity>
    
    @Delete
    suspend fun delete(interaction: QueuedInteractionEntity)
    
    @Delete
    suspend fun delete(interactions: List<QueuedInteractionEntity>)

    @Query("DELETE FROM interaction_queue WHERE row_id IN (:rowIds)")
    suspend fun deleteByIds(rowIds: List<Long>)
    
    @Query("SELECT COUNT(*) FROM interaction_queue")
    suspend fun getQueueSize(): Int
    
    @Query("SELECT * FROM interaction_queue WHERE id = :id AND collectionId = :collectionId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestForContent(id: String, collectionId: String): QueuedInteractionEntity?

    @Query("SELECT * FROM interaction_queue WHERE id = :id AND collectionKind = :collectionKind ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestForContentByKind(id: String, collectionKind: String): QueuedInteractionEntity?
}
