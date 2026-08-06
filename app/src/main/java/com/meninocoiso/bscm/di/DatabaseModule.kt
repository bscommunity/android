package com.meninocoiso.bscm.di

import android.content.Context
import androidx.room.Room
import com.meninocoiso.bscm.data.local.AppDatabase
import com.meninocoiso.bscm.data.local.dao.ChartDao
import com.meninocoiso.bscm.data.local.dao.CollectionDao
import com.meninocoiso.bscm.data.local.dao.InteractionQueueDao
import com.meninocoiso.bscm.data.local.dao.ThemeDao
import com.meninocoiso.bscm.data.local.dao.TourPassDao
import com.meninocoiso.bscm.data.manager.ChartOperationPolicy
import com.meninocoiso.bscm.data.manager.ChartStateMerger
import com.meninocoiso.bscm.data.manager.ContentManager
import com.meninocoiso.bscm.data.manager.ContentMemoryStore
import com.meninocoiso.bscm.data.manager.ThemeManager
import com.meninocoiso.bscm.data.manager.TourPassManager
import com.meninocoiso.bscm.data.manager.TourPassStorageManager
import com.meninocoiso.bscm.data.repository.ChartRepositoryLocal
import com.meninocoiso.bscm.data.repository.ThemeRepositoryLocal
import com.meninocoiso.bscm.data.repository.TourPassRepositoryLocal
import com.meninocoiso.bscm.data.service.FeedOrchestrator
import com.meninocoiso.bscm.domain.enums.SortOption
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.repository.ChartLocalRepository
import com.meninocoiso.bscm.domain.repository.ChartQuery
import com.meninocoiso.bscm.domain.repository.ChartRemoteRepository
import com.meninocoiso.bscm.domain.repository.ContentFeedRepository
import com.meninocoiso.bscm.domain.repository.ContentLocalRepository
import com.meninocoiso.bscm.domain.repository.ContentOperationPolicy
import com.meninocoiso.bscm.domain.repository.ThemeLocalRepository
import com.meninocoiso.bscm.domain.repository.ThemeRemoteRepository
import com.meninocoiso.bscm.domain.repository.TourPassLocalRepository
import com.meninocoiso.bscm.domain.repository.TourPassRemoteRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
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

    @Provides
    fun provideCollectionDao(appDatabase: AppDatabase): CollectionDao {
        return appDatabase.collectionDao()
    }

    @Provides
    fun provideTourPassDao(appDatabase: AppDatabase): TourPassDao {
        return appDatabase.tourPassDao()
    }

    @Provides
    fun provideThemeDao(appDatabase: AppDatabase): ThemeDao {
        return appDatabase.themeDao()
    }

    @Provides
    @Singleton
    fun provideTourPassLocalRepository(
        tourPassDao: TourPassDao
    ): TourPassLocalRepository = TourPassRepositoryLocal(tourPassDao)

    @Provides
    @Singleton
    fun provideThemeLocalRepository(
        themeDao: ThemeDao
    ): ThemeLocalRepository = ThemeRepositoryLocal(themeDao)

    // Chart local repository for chart-specific operations
    @Provides
    @Singleton
    fun provideLocalChartRepository(
        chartDao: ChartDao
    ): ChartLocalRepository = ChartRepositoryLocal(chartDao)

    @Provides
    @Singleton
    fun provideChartLocalContentRepository(
        repository: ChartLocalRepository
    ): ContentLocalRepository<Chart, SortOption, ChartQuery> = repository

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
        remote: ContentFeedRepository<Chart, SortOption, ChartQuery>,
        local: ContentLocalRepository<Chart, SortOption, ChartQuery>,
        remoteItemRepository: ChartRemoteRepository,
        localItemRepository: ChartLocalRepository,
        operationPolicy: ContentOperationPolicy<Chart>,
        suggestionsRepository: ChartRemoteRepository,
        analyticsRepository: ChartRemoteRepository,
        memoryStore: ContentMemoryStore<Chart>,
        @ApplicationScope coroutineScope: CoroutineScope,
        chartStateMerger: ChartStateMerger,
    ): ContentManager<Chart, SortOption, ChartQuery> = ContentManager(
        remoteRepository = remote,
        localRepository = local,
        remoteItemRepository = remoteItemRepository,
        localItemRepository = localItemRepository,
        operationPolicy = operationPolicy,
        suggestionsRepository = suggestionsRepository,
        analyticsRepository = analyticsRepository,
        memoryStore = memoryStore,
        coroutineScope = coroutineScope,
        chartStateMerger = chartStateMerger,
    )

    @Provides
    @Singleton
    fun provideChartOperationPolicy(
        policy: ChartOperationPolicy
    ): ContentOperationPolicy<Chart> = policy

    @Provides
    @Singleton
    fun provideTourPassManager(
        remoteRepository: TourPassRemoteRepository,
        localRepository: TourPassLocalRepository,
        @ApplicationScope coroutineScope: CoroutineScope,
        tourPassStorageManager: TourPassStorageManager
    ): TourPassManager = TourPassManager(
        remoteRepository = remoteRepository,
        localRepository = localRepository,
        coroutineScope = coroutineScope,
        tourPassStorageManager = tourPassStorageManager
    )

    @Provides
    @Singleton
    fun provideThemeManager(
        remoteRepository: ThemeRemoteRepository,
        localRepository: ThemeLocalRepository,
        @ApplicationScope coroutineScope: CoroutineScope,
    ): ThemeManager = ThemeManager(
        remoteRepository = remoteRepository,
        localRepository = localRepository,
        coroutineScope = coroutineScope,
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
            .fallbackToDestructiveMigration(true)
            .build()
    }
}