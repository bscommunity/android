package com.meninocoiso.bscm.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Version

@Dao
interface ChartDao {
    @Query("SELECT * FROM charts WHERE (:query IS NULL OR track LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%') ORDER BY track ASC LIMIT CASE WHEN :limit IS NULL THEN -1 ELSE :limit END OFFSET :offset")
    fun getAll(query: String? = null, limit: Int? = null, offset: Int = 0): List<Chart>

    @Query("SELECT * FROM charts WHERE id = :id")
    fun getChart(id: String): Chart?

    @Query("SELECT * FROM charts WHERE content_id = :contentId LIMIT 1")
    fun getChartByContentId(contentId: String): Chart?

    @Query("SELECT * FROM charts WHERE id IN (:ids)")
    fun getChartsByIds(ids: List<String>): List<Chart>

    @Query("SELECT * FROM charts WHERE liked_at IS NOT NULL ORDER BY liked_at DESC LIMIT :limit OFFSET :offset")
    fun getLikedCharts(limit: Int, offset: Int): List<Chart>

    @Query("SELECT * FROM charts WHERE bookmarked_at IS NOT NULL ORDER BY bookmarked_at DESC LIMIT :limit OFFSET :offset")
    fun getBookmarkedCharts(limit: Int, offset: Int): List<Chart>

    @Query("SELECT id FROM charts WHERE liked_at IS NOT NULL ORDER BY liked_at DESC")
    fun getLikedChartIds(): List<String>

    @Query("SELECT id FROM charts WHERE bookmarked_at IS NOT NULL ORDER BY bookmarked_at DESC")
    fun getBookmarkedChartIds(): List<String>

    //@Query("SELECT latest_version FROM charts WHERE id IN (:ids)")
    @Query("SELECT * from versions WHERE chart_id IN (:ids) ORDER BY `created_at` DESC")
    fun getLatestVersionsByChartIds(ids: List<String>): List<Version>

    @Query("""
    SELECT 
        CASE 
            WHEN track LIKE '%' || :query || '%' THEN track 
            WHEN artist LIKE '%' || :query || '%' THEN artist 
            ELSE album 
        END 
    FROM charts 
    WHERE track LIKE '%' || :query || '%' 
       OR artist LIKE '%' || :query || '%' 
       OR album LIKE '%' || :query || '%' 
    LIMIT CASE WHEN :limit IS NULL THEN -1 ELSE :limit END
""")
    fun getSuggestions(query: String, limit: Int?): List<String>
    /*@Query("SELECT track FROM charts WHERE track LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' LIMIT CASE WHEN :limit IS NULL THEN -1 ELSE :limit END")
    fun getSuggestions(query: String, limit: Int?): List<String>*/

    @Query("""
        SELECT * FROM charts 
        WHERE (:query IS NULL OR track LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%')
        ORDER BY updated_at DESC
        LIMIT CASE WHEN :limit IS NULL THEN -1 ELSE :limit END
        OFFSET :offset
    """)
    fun getChartsSortedByLastUpdatedWithQuery(query: String?, limit: Int?, offset: Int): List<Chart>

    @Query("""
        SELECT * FROM charts 
        WHERE (:query IS NULL OR track LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%')
        ORDER BY downloads_sum DESC
        LIMIT CASE WHEN :limit IS NULL THEN -1 ELSE :limit END
        OFFSET :offset
    """)
    fun getChartsSortedByMostDownloadedWithQuery(query: String?, limit: Int?, offset: Int): List<Chart>
    
    @Query("SELECT * FROM charts WHERE track LIKE :first AND " +
            "artist LIKE :last LIMIT 1")
    fun findByName(first: String, last: String): Chart

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(charts: List<Chart>)

    @Upsert
    fun update(chart: Chart)

    @Upsert
    fun update(chart: List<Chart>)

    /**
     * Updating only is_installed field
     * By chart id
     */
    @Query("UPDATE charts SET is_installed = :isInstalled WHERE id = :id")
    fun update(id: String, isInstalled: Boolean?)

    @Query("UPDATE charts SET latest_version = available_version, available_version = NULL WHERE id = :id")
    fun updateVersion(id: String)

    /**
     * Update the likedAt timestamp for a chart
     * Pass null to remove the like
     */
    @Query("UPDATE charts SET liked_at = :likedAt WHERE id = :chartId")
    suspend fun updateLikedAt(chartId: String, likedAt: String?)

    /**
     * Update the bookmarkedAt timestamp for a chart
     * Pass null to remove the bookmark
     */
    @Query("UPDATE charts SET bookmarked_at = :bookmarkedAt WHERE id = :chartId")
    suspend fun updateBookmarkedAt(chartId: String, bookmarkedAt: String?)

    @Delete
    fun delete(chart: Chart)

    @Delete
    fun delete(charts: List<Chart>)
}