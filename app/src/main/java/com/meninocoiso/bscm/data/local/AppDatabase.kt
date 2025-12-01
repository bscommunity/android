package com.meninocoiso.bscm.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.meninocoiso.bscm.data.local.dao.ChartDao
import com.meninocoiso.bscm.data.local.dao.InteractionQueueDao
import com.meninocoiso.bscm.data.local.entity.QueuedInteractionEntity
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.StreamingLink
import com.meninocoiso.bscm.domain.model.Theme
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.domain.model.Version
import com.meninocoiso.bscm.domain.serialization.Converters

@Database(
    version = 22,
    entities = [
        Chart::class,
        Version::class,
        StreamingLink::class,
        TourPass::class,
        Theme::class,
        QueuedInteractionEntity::class
    ],
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chartDao(): ChartDao
    abstract fun interactionQueueDao(): InteractionQueueDao
}