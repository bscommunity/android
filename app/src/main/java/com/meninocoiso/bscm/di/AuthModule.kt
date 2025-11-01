package com.meninocoiso.bscm.di

import android.content.Context
import com.meninocoiso.bscm.data.manager.CryptoManager
import com.meninocoiso.bscm.data.manager.SecureTokenManager
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.data.repository.AuthRepository
import com.meninocoiso.bscm.data.repository.CacheRepository
import com.meninocoiso.bscm.data.security.DiscordOAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {
    @Provides
    @Singleton
    fun provideDiscordOAuth(
        secureTokenManager: SecureTokenManager
    ): DiscordOAuth {
        return DiscordOAuth(secureTokenManager)
    }

    @Provides
    @Singleton
    fun provideSecureTokenManager(
        @ApplicationContext context: Context,
        cryptoManager: CryptoManager
    ): SecureTokenManager {
        return SecureTokenManager(context, cryptoManager)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        apiClient: ApiClient,
        tokenManager: SecureTokenManager,
        cacheRepository: CacheRepository
    ): AuthRepository {
        return AuthRepository(apiClient, tokenManager, cacheRepository)
    }
}
