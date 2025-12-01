package com.meninocoiso.bscm.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.meninocoiso.bscm.domain.enums.SortOption
import com.meninocoiso.bscm.domain.model.User
import com.meninocoiso.bscm.domain.model.internal.Cache
import com.meninocoiso.bscm.domain.model.internal.ContributionCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    companion object CacheKeys {
        val SEARCH_HISTORY = stringPreferencesKey("search_history")
        val WORKSHOP_SORT = stringPreferencesKey("workshop_sort")
        val USER_JSON = stringPreferencesKey("user_json")
        val CONTRIBUTORS_JSON = stringPreferencesKey("contributors_json")
    }

    private val json = Json { ignoreUnknownKeys = true }

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
    
    suspend fun getLatestWorkshopSort(): SortOption? {
        return dataStore.data.first()[WORKSHOP_SORT]?.let { SortOption.valueOf(it) }
    }

    suspend fun setLatestWorkshopSort(sort: String) {
        dataStore.edit { it[WORKSHOP_SORT] = sort }
    }

    // -------------------- User cache -------------------------
    suspend fun setUser(user: User) {
        val encoded = json.encodeToString(User.serializer(), user)
        dataStore.edit { it[USER_JSON] = encoded }
    }

    suspend fun getUser(): User? {
        val encoded = dataStore.data.first()[USER_JSON] ?: return null
        if (encoded.isBlank()) return null
        return try {
            json.decodeFromString(User.serializer(), encoded)
        } catch (e: SerializationException) {
            Log.e("CacheRepository", "Falha ao decodificar usuário cacheado", e)
            null
        }
    }

    suspend fun clearUser() {
        dataStore.edit { it.remove(USER_JSON) }
    }

    suspend fun setContributors(contributors: List<ContributionCategory>) {
        val jsonStr =
            json.encodeToString(ListSerializer(ContributionCategory.serializer()), contributors)
        dataStore.edit { it[CONTRIBUTORS_JSON] = jsonStr }
    }

    suspend fun getContributors(): List<ContributionCategory> {
        val jsonStr = dataStore.data.first()[CONTRIBUTORS_JSON]
        return if (!jsonStr.isNullOrEmpty()) {
            try {
                json.decodeFromString(jsonStr)
            } catch (e: SerializationException) {
                emptyList()
            }
        } else emptyList()
    }

    private fun mapCache(preferences: Preferences): Cache = Cache(
        searchHistory = preferences[SEARCH_HISTORY]?.split("|") ?: emptyList(),
        latestWorkshopSort = preferences[WORKSHOP_SORT]?.let { SortOption.valueOf(it) }
            ?: Cache().latestWorkshopSort,
        user = preferences[USER_JSON]?.let { encoded ->
            try {
                json.decodeFromString(User.serializer(), encoded)
            } catch (_: Exception) {
                null
            }
        }
    )
}