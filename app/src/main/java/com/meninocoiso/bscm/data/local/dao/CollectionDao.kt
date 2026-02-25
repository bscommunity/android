package com.meninocoiso.bscm.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.meninocoiso.bscm.data.remote.dto.collection.SimplifiedCollection
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Collection
import com.meninocoiso.bscm.domain.model.CollectionItemCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {

    @Query("SELECT * FROM collections WHERE kind = 'USER' ORDER BY updated_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getUserCollections(limit: Int, offset: Int): List<Collection>

    @Query("SELECT * FROM collections WHERE id IN (:ids)")
    suspend fun getCollectionsByIds(ids: List<String>): List<Collection>

    @Query("SELECT id FROM collections WHERE kind = 'USER' ORDER BY updated_at DESC")
    suspend fun getUserCollectionIds(): List<String>

    @Upsert
    suspend fun upsertCollections(collections: List<Collection>)

    @Upsert
    suspend fun upsertCollection(collection: Collection)

    @Query("DELETE FROM collections WHERE id = :collectionId")
    suspend fun deleteCollectionById(collectionId: String)

    @Query("UPDATE collections SET name = COALESCE(:name, name), is_public = COALESCE(:isPublic, is_public), updated_at = :updatedAt WHERE id = :collectionId")
    suspend fun updateCollectionMetadata(
        collectionId: String,
        name: String?,
        isPublic: Boolean?,
        updatedAt: java.time.LocalDateTime
    )

    @Query("UPDATE collections SET chart_count = chart_count + 1, updated_at = :updatedAt WHERE id = :collectionId")
    suspend fun incrementCollectionChartCount(collectionId: String, updatedAt: java.time.LocalDateTime)

    @Query("UPDATE collections SET chart_count = MAX(chart_count - 1, 0), updated_at = :updatedAt WHERE id = :collectionId")
    suspend fun decrementCollectionChartCount(collectionId: String, updatedAt: java.time.LocalDateTime)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCrossRef(crossRef: CollectionItemCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCrossRefs(crossRefs: List<CollectionItemCrossRef>)

    @Upsert
    suspend fun upsertCharts(charts: List<Chart>)

    @Query("DELETE FROM collection_item_cross_ref WHERE collection_id = :collectionId AND content_id = :contentId")
    suspend fun deleteCrossRef(collectionId: String, contentId: String)

    @Query("""
        SELECT id, kind FROM collections c
        INNER JOIN collection_item_cross_ref ref 
            ON c.id = ref.collection_id
        WHERE ref.content_id = :contentId
        LIMIT 1
    """)
    fun getCollectionForContent(contentId: String): Flow<SimplifiedCollection?>

    @Transaction
    @Query("""
        SELECT c.* FROM charts c
        INNER JOIN collection_item_cross_ref ref 
            ON c.content_id = ref.content_id
        WHERE ref.collection_id = :collectionId
        AND ref.content_type = 'CHART'
        ORDER BY ref.added_at DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getChartItems(
        collectionId: String,
        limit: Int,
        offset: Int
    ): List<Chart>

    // Same pattern for TourPass, Theme when those tables exist
}