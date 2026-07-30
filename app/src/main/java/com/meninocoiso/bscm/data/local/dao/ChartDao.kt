package com.meninocoiso.bscm.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Version
import kotlinx.coroutines.flow.Flow

@Dao
interface ChartDao {
    @Query("""
        SELECT * FROM charts 
        WHERE (:query IS NULL OR 
               json_extract(track, '$.title') LIKE '%' || :query || '%' OR 
               json_extract(track, '$.artist') LIKE '%' || :query || '%') 
        ORDER BY json_extract(track, '$.title') ASC 
        LIMIT CASE WHEN :limit IS NULL THEN -1 ELSE :limit END OFFSET :offset
    """)
    fun getAll(query: String? = null, limit: Int? = null, offset: Int = 0): List<Chart>

    @Query("SELECT * FROM charts WHERE id = :id")
    fun getChart(id: String): Chart?

    @Query("SELECT * FROM charts WHERE id IN (:ids)")
    fun getChartsByIds(ids: List<String>): List<Chart>

    @Query("SELECT * FROM charts WHERE liked_at IS NOT NULL ORDER BY liked_at DESC LIMIT :limit OFFSET :offset")
    fun getLikedCharts(limit: Int, offset: Int): List<Chart>

    @Query("""
        SELECT c.* FROM charts c
        INNER JOIN collection_item_cross_ref ref 
            ON c.id = ref.content_id
        WHERE ref.collection_id = 'bookmarks'
        AND ref.content_type = 'CHART'
        ORDER BY ref.added_at DESC
        LIMIT :limit OFFSET :offset
    """)
    fun getBookmarkedCharts(limit: Int, offset: Int): List<Chart>

    @Query("SELECT * FROM charts WHERE liked_at IS NOT NULL ORDER BY liked_at DESC")
    fun observeLikedCharts(): Flow<List<Chart>>

    @Query("""
        SELECT c.* FROM charts c
        INNER JOIN collection_item_cross_ref ref 
            ON c.id = ref.content_id
        WHERE ref.collection_id = 'bookmarks'
        AND ref.content_type = 'CHART'
        ORDER BY ref.added_at DESC
    """)
    fun observeBookmarkedCharts(): Flow<List<Chart>>

    @Query("SELECT id FROM charts WHERE liked_at IS NOT NULL ORDER BY liked_at DESC")
    fun getLikedChartIds(): List<String>

    @Query("""
        SELECT c.id FROM charts c
        INNER JOIN collection_item_cross_ref ref 
            ON c.id = ref.content_id
        WHERE ref.collection_id = 'bookmarks'
        AND ref.content_type = 'CHART'
        ORDER BY ref.added_at DESC
    """)
    fun getBookmarkedChartIds(): List<String>

    @Query("SELECT * from versions WHERE catalog_item_id IN (:ids) ORDER BY version_code DESC")
    fun getLatestVersionsByChartIds(ids: List<String>): List<Version>

    @Query("""
        SELECT DISTINCT 
            CASE 
                WHEN json_extract(track, '$.title') LIKE '%' || :query || '%' THEN json_extract(track, '$.title') 
                WHEN json_extract(track, '$.artist') LIKE '%' || :query || '%' THEN json_extract(track, '$.artist') 
                ELSE json_extract(track, '$.album') 
            END 
        FROM charts 
        WHERE json_extract(track, '$.title') LIKE '%' || :query || '%' 
           OR json_extract(track, '$.artist') LIKE '%' || :query || '%' 
           OR json_extract(track, '$.album') LIKE '%' || :query || '%' 
        LIMIT CASE WHEN :limit IS NULL THEN -1 ELSE :limit END
    """)
    fun getSuggestions(query: String, limit: Int?): List<String>

    @Query("""
        SELECT * FROM charts 
        WHERE (:query IS NULL OR 
               json_extract(track, '$.title') LIKE '%' || :query || '%' OR 
               json_extract(track, '$.artist') LIKE '%' || :query || '%')
        ORDER BY updated_at DESC
        LIMIT CASE WHEN :limit IS NULL THEN -1 ELSE :limit END
        OFFSET :offset
    """)
    fun getChartsSortedByLastUpdatedWithQuery(query: String?, limit: Int?, offset: Int): List<Chart>

    @Query("""
        SELECT * FROM charts 
        WHERE (:query IS NULL OR 
               json_extract(track, '$.title') LIKE '%' || :query || '%' OR 
               json_extract(track, '$.artist') LIKE '%' || :query || '%')
        ORDER BY downloads_sum DESC
        LIMIT CASE WHEN :limit IS NULL THEN -1 ELSE :limit END
        OFFSET :offset
    """)
    fun getChartsSortedByMostDownloadedWithQuery(query: String?, limit: Int?, offset: Int): List<Chart>

    @Query("SELECT * FROM charts WHERE json_extract(track, '$.title') LIKE :first AND " +
            "json_extract(track, '$.artist') LIKE :last LIMIT 1")
    fun findByName(first: String, last: String): Chart

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(charts: List<Chart>)

    @Upsert
    fun update(chart: Chart)

    @Upsert
    fun update(chart: List<Chart>)

    @Query("UPDATE charts SET is_installed = :isInstalled WHERE id = :id")
    fun update(id: String, isInstalled: Boolean?)

    @Query("UPDATE charts SET latest_version = available_version, available_version = NULL WHERE id = :id")
    fun updateVersion(id: String)

    @Query("UPDATE charts SET liked_at = :likedAt WHERE id = :chartId")
    suspend fun updateLikedAt(chartId: String, likedAt: String?)

    @Query("UPDATE charts SET bookmarked_at = :bookmarkedAt WHERE id = :chartId")
    suspend fun updateBookmarkedAt(chartId: String, bookmarkedAt: String?)

    @Delete
    fun delete(chart: Chart)

    @Delete
    fun delete(charts: List<Chart>)
}
