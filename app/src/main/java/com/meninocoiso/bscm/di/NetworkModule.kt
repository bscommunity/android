package com.meninocoiso.bscm.di

import android.content.Context
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.data.remote.KtorApiClient
import com.meninocoiso.bscm.data.repository.ChartContentRepositoryRemote
import com.meninocoiso.bscm.data.repository.ChartRepositoryRemote
import com.meninocoiso.bscm.data.security.AuthInterceptor
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.repository.ChartRepository
import com.meninocoiso.bscm.domain.repository.ContentRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object NetworkModule {
    @Provides
    @Singleton
    fun provideApiClient(
        @ApplicationContext context: Context,
        interceptor: AuthInterceptor
    ): ApiClient = KtorApiClient(context, interceptor)

    // Keep the old ChartRepository for ChartManager's chart-specific operations
    @Provides
    @Singleton
    @Named("Remote")
    fun provideChartRepository(
        apiClient: ApiClient
    ): ChartRepository = ChartRepositoryRemote(apiClient)

    // ContentRepository adapter for generic operations
    @Provides
    @Singleton
    @Named("Remote")
    fun provideChartContentRepositoryRemote(
        adapter: ChartContentRepositoryRemote
    ): ContentRepository<Chart> = adapter

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