package com.meninocoiso.bscm.data.repository

import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.meninocoiso.bscm.domain.enums.SortOption
import com.meninocoiso.bscm.domain.model.internal.Cache
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheRepository @Inject constructor(
	private val dataStore: DataStore<Preferences>,
) {
	companion object CacheKeys {
		val SEARCH_HISTORY = stringPreferencesKey("search_history")
		val FOLDER_URI = stringPreferencesKey("folder_uri")
		val LATEST_WORKSHOP_SORT = stringPreferencesKey("latest_workshop_sort")
	}

	val cacheFlow: Flow<Cache> = dataStore.data
		.catch { exception ->
			when (exception) {
				is IOException -> {
					Log.e("CacheRepository", "Error reading cache", exception)
					emit(emptyPreferences())
				}
				else -> throw exception
			}
		}
		.map { preferences ->
			mapCache(preferences)
		}

	suspend fun setSearchHistory(songs: List<String>) {
		val serializedSongs = songs.joinToString("|") // "|" is the delimiter
		dataStore.edit { it[SEARCH_HISTORY] = serializedSongs }
	}

	suspend fun getSearchHistory(): List<String> {
		val serializedSongs = dataStore.data.first()[SEARCH_HISTORY] ?: ""
		return if (serializedSongs.isNotEmpty()) serializedSongs.split("|") else emptyList()
	}

	suspend fun setFolderUri(uri: String) =
		dataStore.edit { it[FOLDER_URI] = uri }

	suspend fun getFolderUri(): Uri? {
		return dataStore.data.first()[FOLDER_URI]?.toUri()
	}
	
	suspend fun getLatestWorkshopSort(): SortOption? {
		return dataStore.data.first()[LATEST_WORKSHOP_SORT]?.let { SortOption.valueOf(it) }
	}

	suspend fun setLatestWorkshopSort(sort: String) {
		dataStore.edit { it[LATEST_WORKSHOP_SORT] = sort }
	}

	private fun mapCache(preferences: Preferences): Cache = Cache(
		searchHistory = preferences[SEARCH_HISTORY]?.split("|") ?: emptyList(),
		folderUri = preferences[FOLDER_URI]
			?: Cache().folderUri,
		latestWorkshopSort = preferences[LATEST_WORKSHOP_SORT]?.let { SortOption.valueOf(it) } ?: Cache().latestWorkshopSort
	)
}