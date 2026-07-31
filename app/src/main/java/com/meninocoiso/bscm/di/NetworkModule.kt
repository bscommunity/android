package com.meninocoiso.bscm.di

import android.content.Context
import com.meninocoiso.bscm.data.manager.InteractionQueueManager
import com.meninocoiso.bscm.data.manager.SecureTokenManager
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.data.remote.KtorApiClient
import com.meninocoiso.bscm.data.repository.ChartRepositoryRemote
import com.meninocoiso.bscm.data.repository.ThemeRepositoryRemote
import com.meninocoiso.bscm.data.repository.TourPassRepositoryRemote
import com.meninocoiso.bscm.data.security.AuthInterceptor
import com.meninocoiso.bscm.domain.enums.SortOption
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.repository.ChartQuery
import com.meninocoiso.bscm.domain.repository.ChartRemoteRepository
import com.meninocoiso.bscm.domain.repository.ContentFeedRepository
import com.meninocoiso.bscm.domain.repository.ThemeRemoteRepository
import com.meninocoiso.bscm.domain.repository.TourPassRemoteRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object NetworkModule {
    @Provides
    @Singleton
    fun provideApiClient(
        @ApplicationContext context: Context,
        interceptor: AuthInterceptor,
        tokenManager: SecureTokenManager
    ): ApiClient = KtorApiClient(context, interceptor, tokenManager)

    // Chart remote repository for chart-specific operations
    @Provides
    @Singleton
    fun provideChartRepository(
        apiClient: ApiClient,
        queueManager: InteractionQueueManager
    ): ChartRemoteRepository = ChartRepositoryRemote(apiClient, queueManager)

    @Provides
    @Singleton
    fun provideChartFeedRepository(
        repository: ChartRemoteRepository
    ): ContentFeedRepository<Chart, SortOption, ChartQuery> = repository

    @Provides
    @Singleton
    fun provideTourPassRepository(
        apiClient: ApiClient
    ): TourPassRemoteRepository = TourPassRepositoryRemote(apiClient)

    @Provides
    @Singleton
    fun provideThemeRepository(
        apiClient: ApiClient
    ): ThemeRemoteRepository = ThemeRepositoryRemote(apiClient)

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
}