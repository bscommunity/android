package com.meninocoiso.bscm.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

private const val SETTINGS_NAME = "settings"

@InstallIn(SingletonComponent::class)
@Module
object DataStoreModule {

	/**
	 * Provides a singleton instance of DataStore<Preferences>.
	 *
	 * @param appContext The application context used to create the DataStore.
	 * @return A DataStore<Preferences> instance.
	 */
	@Singleton
	@Provides
	fun provideDataStore(@ApplicationContext appContext: Context): DataStore<Preferences> {
		return PreferenceDataStoreFactory.create(
			corruptionHandler = null, // No corruption handler is provided.
			migrations = listOf(SharedPreferencesMigration(appContext, SETTINGS_NAME)), // Migrate from SharedPreferences.
			scope = CoroutineScope(Dispatchers.IO + SupervisorJob()), // Use IO dispatcher with a SupervisorJob.
			produceFile = { appContext.preferencesDataStoreFile(SETTINGS_NAME) } // Produce the file for DataStore.
		)
	}
}