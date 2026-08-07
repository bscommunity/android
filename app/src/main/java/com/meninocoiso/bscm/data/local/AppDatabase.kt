package com.meninocoiso.bscm.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.meninocoiso.bscm.data.local.dao.ChartDao
import com.meninocoiso.bscm.data.local.dao.CollectionDao
import com.meninocoiso.bscm.data.local.dao.InteractionQueueDao
import com.meninocoiso.bscm.data.local.dao.ThemeDao
import com.meninocoiso.bscm.data.local.dao.TourPassDao
import com.meninocoiso.bscm.data.local.entity.QueuedInteractionEntity
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Collection
import com.meninocoiso.bscm.domain.model.CollectionItemCrossRef
import com.meninocoiso.bscm.domain.model.StreamingRef
import com.meninocoiso.bscm.domain.model.Theme
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.domain.model.Version
import com.meninocoiso.bscm.domain.serialization.RoomSerializers

@Database(
    version = 37,
    entities = [
        Chart::class,
        Version::class,
        StreamingRef::class,
        TourPass::class,
        Theme::class,
        Collection::class,
        CollectionItemCrossRef::class,
        QueuedInteractionEntity::class
    ],
    exportSchema = false
)
@TypeConverters(RoomSerializers::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chartDao(): ChartDao
    abstract fun collectionDao(): CollectionDao
    abstract fun interactionQueueDao(): InteractionQueueDao
    abstract fun tourPassDao(): TourPassDao
    abstract fun themeDao(): ThemeDao
}
