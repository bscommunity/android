package com.meninocoiso.beatstarcommunity.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.domain.model.Version

@Dao
interface ChartDao {
    @Query("SELECT * FROM charts WHERE (:query IS NULL OR track LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%') LIMIT :limit OFFSET :offset")
    fun getAll(query: String?, limit: Int?, offset: Int): List<Chart>

    @Query("SELECT * FROM charts WHERE id = :id")
    fun getChart(id: String): Chart

    @Query("SELECT * FROM charts WHERE id IN (:chartIds)")
    fun loadAllByIds(chartIds: List<String>): List<Chart>

    //@Query("SELECT latest_version FROM charts WHERE id IN (:ids)")
    @Query("SELECT v.* FROM charts c JOIN versions v ON c.latest_version = v.id WHERE c.id IN (:ids)")
    fun getLatestVersionsByChartIds(ids: List<String>): List<Version>

    @Query("SELECT * FROM charts WHERE track LIKE :first AND " +
            "artist LIKE :last LIMIT 1")
    fun findByName(first: String, last: String): Chart

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(charts: List<Chart>)

    @Update
    fun update(chart: Chart)

    @Update
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