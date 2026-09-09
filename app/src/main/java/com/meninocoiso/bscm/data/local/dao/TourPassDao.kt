package com.meninocoiso.bscm.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.meninocoiso.bscm.domain.model.TourPass
import kotlinx.coroutines.flow.Flow

@Dao
interface TourPassDao {
    @Query("""
        SELECT * FROM tour_passes 
        WHERE (:query IS NULL OR 
               name LIKE '%' || :query || '%' OR 
               description LIKE '%' || :query || '%') 
        ORDER BY updated_at DESC 
        LIMIT CASE WHEN :limit IS NULL THEN -1 ELSE :limit END OFFSET :offset
    """)
    fun getTourPasses(query: String? = null, limit: Int? = null, offset: Int = 0): List<TourPass>

    @Query("SELECT * FROM tour_passes WHERE id = :id")
    fun getTourPass(id: String): TourPass?

    @Query("SELECT * FROM tour_passes ORDER BY updated_at DESC")
    fun observeTourPasses(): Flow<List<TourPass>>

    @Query("SELECT * FROM tour_passes WHERE id IN (:ids)")
    fun getTourPassesByIds(ids: List<String>): List<TourPass>

    @Query("""
        SELECT * FROM tour_passes 
        WHERE liked_at IS NOT NULL 
        ORDER BY liked_at DESC
        LIMIT :limit OFFSET :offset
    """)
    fun getLikedTourPasses(limit: Int, offset: Int): List<TourPass>

    @Query("SELECT COUNT(*) FROM tour_passes WHERE liked_at IS NOT NULL")
    fun countLikedTourPasses(): Int

    @Query("""
        SELECT COUNT(*) FROM tour_passes tp
        INNER JOIN collection_item_cross_ref ref 
            ON tp.id = ref.content_id
        WHERE ref.collection_id = 'bookmarks'
        AND ref.content_type = 'TOUR_PASS'
    """)
    fun countBookmarkedTourPasses(): Int

    @Query("""
        SELECT * FROM tour_passes 
        WHERE liked_at IS NOT NULL 
        ORDER BY liked_at DESC
    """)
    fun observeLikedTourPasses(): Flow<List<TourPass>>

    @Query("""
        SELECT tp.* FROM tour_passes tp
        INNER JOIN collection_item_cross_ref ref 
            ON tp.id = ref.content_id
        WHERE ref.collection_id = 'bookmarks'
        AND ref.content_type = 'TOUR_PASS'
        ORDER BY ref.added_at DESC
        LIMIT :limit OFFSET :offset
    """)
    fun getBookmarkedTourPasses(limit: Int, offset: Int): List<TourPass>

    @Query("""
        SELECT tp.* FROM tour_passes tp
        INNER JOIN collection_item_cross_ref ref 
            ON tp.id = ref.content_id
        WHERE ref.collection_id = 'bookmarks'
        AND ref.content_type = 'TOUR_PASS'
        ORDER BY ref.added_at DESC
    """)
    fun observeBookmarkedTourPasses(): Flow<List<TourPass>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tourPasses: List<TourPass>)

    @Upsert
    fun update(tourPass: TourPass)

    @Upsert
    fun update(tourPasses: List<TourPass>)

    @Query("UPDATE tour_passes SET liked_at = :likedAt WHERE id = :tourPassId")
    suspend fun updateLikedAt(tourPassId: String, likedAt: Long?)

    @Query("UPDATE tour_passes SET bookmarked_at = :bookmarkedAt WHERE id = :tourPassId")
    suspend fun updateBookmarkedAt(tourPassId: String, bookmarkedAt: Long?)

    @Delete
    fun delete(tourPass: TourPass)

    @Delete
    fun delete(tourPasses: List<TourPass>)
}
