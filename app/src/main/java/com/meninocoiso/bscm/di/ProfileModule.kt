package com.meninocoiso.bscm.di

import com.meninocoiso.bscm.data.repository.CollectionRepositoryRemote
import com.meninocoiso.bscm.data.repository.MeRepositoryRemote
import com.meninocoiso.bscm.data.repository.ProfileRepositoryRemote
import com.meninocoiso.bscm.domain.repository.CollectionRepository
import com.meninocoiso.bscm.domain.repository.MeRepository
import com.meninocoiso.bscm.domain.repository.ProfileRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ProfileModule {
    @Provides
    @Singleton
    fun provideProfileRepository(
        repository: ProfileRepositoryRemote
    ): ProfileRepository = repository

    @Provides
    @Singleton
    fun provideMeRepository(
        repository: MeRepositoryRemote
    ): MeRepository = repository

    @Provides
    @Singleton
    fun provideCollectionRepository(
        repository: CollectionRepositoryRemote
    ): CollectionRepository = repository
}
