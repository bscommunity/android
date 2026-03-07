package com.meninocoiso.bscm.di

import android.content.Context
import com.meninocoiso.bscm.data.local.dao.CollectionDao
import com.meninocoiso.bscm.data.local.dao.InteractionQueueDao
import com.meninocoiso.bscm.data.manager.ChartManager
import com.meninocoiso.bscm.data.manager.InteractionQueueManager
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.data.repository.InteractionRepositoryImpl
import com.meninocoiso.bscm.domain.repository.InteractionRepository
import com.meninocoiso.bscm.monitor.NetworkConnectivityMonitor
import com.meninocoiso.bscm.service.InteractionSyncService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object InteractionModule {

    @Provides
    @Singleton
    fun provideNetworkConnectivityMonitor(
        @ApplicationContext context: Context
    ): NetworkConnectivityMonitor = NetworkConnectivityMonitor(context)

    @Provides
    @Singleton
    fun provideInteractionQueueManager(
        queueDao: InteractionQueueDao,
        apiClient: ApiClient,
        networkMonitor: NetworkConnectivityMonitor,
        @ApplicationScope applicationScope: CoroutineScope
    ): InteractionQueueManager =
        InteractionQueueManager(queueDao, apiClient, networkMonitor, applicationScope)

    @Provides
    @Singleton
    fun provideInteractionSyncService(
        networkMonitor: NetworkConnectivityMonitor,
        queueManager: InteractionQueueManager
    ): InteractionSyncService = InteractionSyncService(networkMonitor, queueManager)

    @Provides
    @Singleton
    fun provideInteractionRepository(
        queueManager: InteractionQueueManager,
        chartManager: ChartManager,
        collectionDao: CollectionDao,
    ): InteractionRepository = InteractionRepositoryImpl(
        queueManager,
        chartManager,
        collectionDao,
    )
}
