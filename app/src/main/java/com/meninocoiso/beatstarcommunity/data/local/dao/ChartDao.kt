package com.meninocoiso.beatstarcommunity.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.meninocoiso.beatstarcommunity.domain.model.Chart

@Dao
interface ChartDao {
    @Query("SELECT * FROM charts")
    fun getAll(): List<Chart>

    @Query("SELECT * FROM charts WHERE id IN (:chartIds)")
    fun loadAllByIds(chartIds: List<String>): List<Chart>

    @Query("SELECT * FROM charts WHERE track LIKE :first AND " +
            "artist LIKE :last LIMIT 1")
    fun findByName(first: String, last: String): Chart

    @Insert
    fun insertAll(vararg charts: Chart)

    @Delete
    fun delete(chart: Chart)
}