package com.meninocoiso.beatstarcommunity.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.meninocoiso.beatstarcommunity.data.local.dao.ChartDao
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.domain.model.Version
import com.meninocoiso.beatstarcommunity.domain.serialization.Converters

@Database(
    version = 7,
    entities = [
        Chart::class,
        Version::class,
    ],
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chartDao(): ChartDao
}