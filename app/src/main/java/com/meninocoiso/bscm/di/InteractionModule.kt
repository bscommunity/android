package com.meninocoiso.bscm.di

import android.content.Context
import com.meninocoiso.bscm.data.local.dao.InteractionQueueDao
import com.meninocoiso.bscm.data.manager.InteractionQueueManager
import com.meninocoiso.bscm.data.manager.InteractionSyncService
import com.meninocoiso.bscm.data.manager.NetworkConnectivityMonitor
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.data.repository.InteractionRepository
import com.meninocoiso.bscm.data.repository.InteractionRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
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
        @ApplicationContext context: Context
    ): InteractionQueueManager = InteractionQueueManager(queueDao, apiClient, context)
    
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
        apiClient: ApiClient
    ): InteractionRepository = InteractionRepositoryImpl(queueManager, apiClient)
}
