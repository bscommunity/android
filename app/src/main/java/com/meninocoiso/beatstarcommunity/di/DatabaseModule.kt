package com.meninocoiso.beatstarcommunity.di

import android.content.Context
import androidx.room.Room
import com.meninocoiso.beatstarcommunity.data.local.AppDatabase
import com.meninocoiso.beatstarcommunity.data.local.dao.ChartDao
import com.meninocoiso.beatstarcommunity.data.repository.ChartRepository
import com.meninocoiso.beatstarcommunity.data.repository.ChartRepositoryLocal
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    fun provideChartDao(appDatabase: AppDatabase): ChartDao {
        return appDatabase.chartDao()
    }

    @Provides
    @Singleton
    @Named("Local")
    fun provideLocalChartRepository(
        chartDao: ChartDao
    ): ChartRepository = ChartRepositoryLocal(chartDao)

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context = context,
            AppDatabase::class.java,
            "chart_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
}