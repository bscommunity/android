package com.meninocoiso.bscm.di

import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.data.remote.KtorApiClient
import com.meninocoiso.bscm.data.repository.ChartRepository
import com.meninocoiso.bscm.data.repository.ChartRepositoryRemote
import com.meninocoiso.bscm.data.repository.UserRepository
import com.meninocoiso.bscm.data.repository.UserRepositoryRemote
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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