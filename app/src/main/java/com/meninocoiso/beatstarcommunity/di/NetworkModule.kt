package com.meninocoiso.beatstarcommunity.di

import com.meninocoiso.beatstarcommunity.data.remote.ApiClient
import com.meninocoiso.beatstarcommunity.data.remote.KtorApiClient
import com.meninocoiso.beatstarcommunity.data.repository.ChartRepository
import com.meninocoiso.beatstarcommunity.data.repository.ChartRepositoryImpl
import com.meninocoiso.beatstarcommunity.data.repository.UserRepository
import com.meninocoiso.beatstarcommunity.data.repository.UserRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object NetworkModule {
    @Provides
    @Singleton
    fun provideApiClient(): ApiClient = KtorApiClient()

    @Provides
    @Singleton
    fun provideUserRepository(
        apiClient: ApiClient
    ): UserRepository = UserRepositoryImpl(apiClient)

    @Provides
    @Singleton
    fun provideChartRepository(
        apiClient: ApiClient
    ): ChartRepository = ChartRepositoryImpl(apiClient)
}