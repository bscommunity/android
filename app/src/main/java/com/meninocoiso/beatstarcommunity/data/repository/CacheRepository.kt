package com.meninocoiso.beatstarcommunity.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.meninocoiso.beatstarcommunity.domain.model.Cache
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

	private fun mapCache(preferences: Preferences): Cache = Cache(
		searchHistory = preferences[SEARCH_HISTORY]?.split("|") ?: emptyList()
	)
}