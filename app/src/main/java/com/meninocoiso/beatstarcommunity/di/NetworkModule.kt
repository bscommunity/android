package com.meninocoiso.beatstarcommunity.di

import com.meninocoiso.beatstarcommunity.data.remote.ApiClient
import com.meninocoiso.beatstarcommunity.data.remote.KtorApiClient
import com.meninocoiso.beatstarcommunity.data.repository.ChartRepository
import com.meninocoiso.beatstarcommunity.data.repository.ChartRepositoryRemote
import com.meninocoiso.beatstarcommunity.data.repository.UserRepository
import com.meninocoiso.beatstarcommunity.data.repository.UserRepositoryRemote
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
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
    ): UserRepository = UserRepositoryRemote(apiClient)

    @Provides
    @Singleton
    @Named("Remote")
    fun provideChartRepository(
        apiClient: ApiClient
    ): ChartRepository = ChartRepositoryRemote(apiClient)
}