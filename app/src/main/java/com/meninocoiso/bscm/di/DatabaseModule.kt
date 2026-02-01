package com.meninocoiso.bscm.di

import android.content.Context
import androidx.room.Room
import com.meninocoiso.bscm.data.local.AppDatabase
import com.meninocoiso.bscm.data.local.dao.ChartDao
import com.meninocoiso.bscm.data.local.dao.InteractionQueueDao
import com.meninocoiso.bscm.data.manager.ContentManager
import com.meninocoiso.bscm.data.manager.ContentMemoryStore
import com.meninocoiso.bscm.data.repository.ChartContentRepositoryLocal
import com.meninocoiso.bscm.data.repository.ChartRepositoryLocal
import com.meninocoiso.bscm.data.service.FeedOrchestrator
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.repository.ChartRepository
import com.meninocoiso.bscm.domain.repository.ContentRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    fun provideChartDao(appDatabase: AppDatabase): ChartDao {
        return appDatabase.chartDao()
    }

    @Provides
    fun provideInteractionQueueDao(appDatabase: AppDatabase): InteractionQueueDao {
        return appDatabase.interactionQueueDao()
    }

    // Keep the old ChartRepository for ChartManager's chart-specific operations
    @Provides
    @Singleton
    @Named("Local")
    fun provideLocalChartRepository(
        chartDao: ChartDao
    ): ChartRepository = ChartRepositoryLocal(chartDao)

    // ContentRepository adapter for generic operations
    @Provides
    @Singleton
    @Named("Local")
    fun provideChartContentRepositoryLocal(
        adapter: ChartContentRepositoryLocal
    ): ContentRepository<Chart> = adapter

    @Provides
    @Singleton
    fun provideFeedOrchestrator(): FeedOrchestrator<Chart> = FeedOrchestrator()

    @Provides
    @Singleton
    fun provideContentMemoryStore(feedOrchestrator: FeedOrchestrator<Chart>): ContentMemoryStore<Chart> =
        ContentMemoryStore(feedOrchestrator)

    @Provides
    @Singleton
    fun provideChartContentManager(
        @ApplicationContext context: Context,
        @Named("Remote") remote: ContentRepository<Chart>,
        @Named("Local") local: ContentRepository<Chart>,
        memoryStore: ContentMemoryStore<Chart>,
        @ApplicationScope coroutineScope: CoroutineScope
    ): ContentManager<Chart> = ContentManager(
        context = context,
        remoteRepository = remote,
        localRepository = local,
        memoryStore = memoryStore,
        coroutineScope = coroutineScope
    )

    /**
     * Provides a singleton instance of AppDatabase.
     *
     * @param context The application context used to create the database.
     * @return An instance of AppDatabase.
     */
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context = context,
            AppDatabase::class.java,
            "local_database"
        )
            .fallbackToDestructiveMigration(false)
            .build()
    }
}