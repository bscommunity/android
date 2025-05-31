package com.meninocoiso.bscm.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.meninocoiso.bscm.data.local.dao.ChartDao
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Version
import com.meninocoiso.bscm.domain.serialization.Converters

@Database(
    version = 10,
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