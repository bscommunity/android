package com.meninocoiso.beatstarcommunity.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.domain.model.Version

@Dao
interface ChartDao {
    @Query("SELECT * FROM charts WHERE (:query IS NULL OR track LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%') ORDER BY track ASC LIMIT CASE WHEN :limit IS NULL THEN -1 ELSE :limit END OFFSET :offset")
    fun getAll(query: String? = null, limit: Int? = null, offset: Int = 0): List<Chart>

    @Query("SELECT * FROM charts WHERE id = :id")
    fun getChart(id: String): Chart

    //@Query("SELECT latest_version FROM charts WHERE id IN (:ids)")
    @Query("SELECT v.* FROM charts c JOIN versions v ON c.latest_version = v.id WHERE c.id IN (:ids)")
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
        SELECT c.* FROM charts c
        ORDER BY c.latest_published_at DESC
        LIMIT CASE WHEN :limit IS NULL THEN -1 ELSE :limit END
        OFFSET :offset
    """)
    fun getChartsSortedByLastUpdated(limit: Int?, offset: Int): List<Chart>

    @Query("""
        SELECT * FROM charts
        ORDER BY downloads_sum DESC
        LIMIT CASE WHEN :limit IS NULL THEN -1 ELSE :limit END
        OFFSET :offset
    """)
    fun getChartsSortedByMostDownloaded(limit: Int?, offset: Int): List<Chart>
    
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

    @Delete
    fun delete(chart: Chart)

    @Delete
    fun delete(charts: List<Chart>)
}