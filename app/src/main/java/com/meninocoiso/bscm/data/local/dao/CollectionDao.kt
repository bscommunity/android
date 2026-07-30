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
import java.time.LocalDateTime

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

    @Query("UPDATE collections SET name = COALESCE(:name, name), is_public = COALESCE(:isPublic, is_public), slug = :slug, updated_at = :updatedAt WHERE id = :collectionId")
    suspend fun updateCollectionMetadata(
        collectionId: String,
        name: String?,
        isPublic: Boolean?,
        slug: String?,
        updatedAt: LocalDateTime
    )


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCrossRef(crossRef: CollectionItemCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCrossRefs(crossRefs: List<CollectionItemCrossRef>)

    @Upsert
    suspend fun upsertCharts(charts: List<Chart>)

    @Query("DELETE FROM collection_item_cross_ref WHERE collection_id = :collectionId AND content_id = :contentId")
    suspend fun deleteCrossRef(collectionId: String, contentId: String)

    @Query("DELETE FROM collection_item_cross_ref WHERE content_id = :contentId AND collection_id IN (SELECT id FROM collections WHERE kind = 'USER')")
    suspend fun deleteUserCrossRefsForContent(contentId: String)

    @Query("SELECT collection_id FROM collection_item_cross_ref WHERE content_id = :contentId AND collection_id IN (SELECT id FROM collections WHERE kind = 'USER')")
    suspend fun getUserCollectionIdsForContent(contentId: String): List<String>

    /**
     * Removes all cross-refs for [collectionId] whose content_id is NOT in [retainedContentIds].
     * Use this after a full remote sync to evict stale local entries (e.g. items moved out of
     * Bookmarks into a custom collection that the server no longer returns in the bookmarks list).
     */
    @Query("DELETE FROM collection_item_cross_ref WHERE collection_id = :collectionId AND content_id NOT IN (:retainedContentIds)")
    suspend fun deleteStaleCrossRefs(collectionId: String, retainedContentIds: List<String>)

    /**
     * Removes ALL cross-refs for [collectionId]. Used when the server returns an empty list
     * (so retainedContentIds would be empty, which is not valid for a SQL IN clause).
     */
    @Query("DELETE FROM collection_item_cross_ref WHERE collection_id = :collectionId")
    suspend fun deleteAllCrossRefsForCollection(collectionId: String)

    @Query("SELECT * FROM collections WHERE kind = 'USER' ORDER BY updated_at DESC")
    fun observeUserCollections(): Flow<List<Collection>>

    @Transaction
    @Query(
        """
        SELECT c.* FROM charts c
        INNER JOIN collection_item_cross_ref ref 
            ON c.id = ref.content_id
        WHERE ref.collection_id = :collectionId
        AND ref.content_type = 'CHART'
        ORDER BY ref.added_at DESC
        LIMIT :limit OFFSET :offset
    """
    )
    suspend fun getChartItems(
        collectionId: String,
        limit: Int,
        offset: Int
    ): List<Chart>

    @Query(
        """
        SELECT content_id FROM collection_item_cross_ref
        WHERE collection_id = :collectionId
        AND content_type = 'CHART'
        ORDER BY added_at DESC
    """
    )
    fun observeChartContentIdsForCollection(collectionId: String): Flow<List<String>>

    @Query(
        """
        SELECT c.id, c.kind FROM collections c
        INNER JOIN collection_item_cross_ref ref 
            ON c.id = ref.collection_id
        WHERE ref.content_id = :contentId
        ORDER BY CASE WHEN c.kind = 'BOOKMARKS' THEN 0 ELSE 1 END, ref.added_at DESC
    """
    )
    fun getCollectionsForContent(contentId: String): Flow<List<SimplifiedCollection>>

    @Query("SELECT EXISTS(SELECT 1 FROM collection_item_cross_ref WHERE collection_id = :collectionId AND content_id = :contentId)")
    suspend fun hasCrossRef(collectionId: String, contentId: String): Boolean

    // Same pattern for TourPass, Theme when those tables exist
}