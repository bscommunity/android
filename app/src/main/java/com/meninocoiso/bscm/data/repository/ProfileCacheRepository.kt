package com.meninocoiso.bscm.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.meninocoiso.bscm.data.remote.dto.activity.ActivityEntry
import com.meninocoiso.bscm.data.remote.dto.user.UserProfileResponse
import com.meninocoiso.bscm.domain.model.Collection
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ProfileCacheRepository"
private const val CACHE_EXPIRATION_MILLIS = 30 * 60 * 1000L // 30 minutes

@Singleton
class ProfileCacheRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    companion object ProfileCacheKeys {
        // Profile cache keys
        fun profileKey(userId: String) = stringPreferencesKey("profile_$userId")
        fun profileTimestampKey(userId: String) = longPreferencesKey("profile_timestamp_$userId")
        fun collectionsKey(userId: String) = stringPreferencesKey("collections_$userId")
        fun activityKey(userId: String) = stringPreferencesKey("activity_$userId")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // -------------------- Profile --------------------
    suspend fun cacheProfile(userId: String, profile: UserProfileResponse) {
        try {
            val encoded = json.encodeToString(UserProfileResponse.serializer(), profile)
            dataStore.edit { preferences ->
                preferences[profileKey(userId)] = encoded
                preferences[profileTimestampKey(userId)] = System.currentTimeMillis()
            }
            Log.d(TAG, "Cached profile for user: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching profile for user: $userId", e)
        }
    }

    suspend fun getProfile(userId: String): UserProfileResponse? {
        return try {
            val preferences = dataStore.data.first()
            val timestamp = preferences[profileTimestampKey(userId)] ?: 0L

            if (!isCacheValid(timestamp)) {
                Log.d(TAG, "Profile cache expired for user: $userId")
                invalidateProfile(userId)
                return null
            }

            val encoded = preferences[profileKey(userId)] ?: return null
            json.decodeFromString(UserProfileResponse.serializer(), encoded)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading profile cache for user: $userId", e)
            null
        }
    }

    // -------------------- Collections --------------------
    suspend fun cacheCollections(userId: String, collections: List<Collection>) {
        try {
            val encoded = json.encodeToString(ListSerializer(Collection.serializer()), collections)
            dataStore.edit { preferences ->
                preferences[collectionsKey(userId)] = encoded
            }
            Log.d(TAG, "Cached collections for user: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching collections for user: $userId", e)
        }
    }

    suspend fun getCollections(userId: String): List<Collection>? {
        return try {
            val preferences = dataStore.data.first()
            val timestamp = preferences[profileTimestampKey(userId)] ?: 0L

            if (!isCacheValid(timestamp)) {
                Log.d(TAG, "Collections cache expired for user: $userId")
                invalidateProfile(userId)
                return null
            }

            val encoded = preferences[collectionsKey(userId)] ?: return null
            json.decodeFromString(ListSerializer(Collection.serializer()), encoded)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading collections cache for user: $userId", e)
            null
        }
    }

    // -------------------- Activity --------------------
    suspend fun cacheActivity(userId: String, activity: List<ActivityEntry>) {
        try {
            val encoded = json.encodeToString(ListSerializer(ActivityEntry.serializer()), activity)
            dataStore.edit { preferences ->
                preferences[activityKey(userId)] = encoded
            }
            Log.d(TAG, "Cached activity for user: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching activity for user: $userId", e)
        }
    }

    suspend fun getActivity(userId: String): List<ActivityEntry>? {
        return try {
            val preferences = dataStore.data.first()
            val timestamp = preferences[profileTimestampKey(userId)] ?: 0L

            if (!isCacheValid(timestamp)) {
                Log.d(TAG, "Activity cache expired for user: $userId")
                invalidateProfile(userId)
                return null
            }

            val encoded = preferences[activityKey(userId)] ?: return null
            json.decodeFromString(ListSerializer(ActivityEntry.serializer()), encoded)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading activity cache for user: $userId", e)
            null
        }
    }

    // -------------------- Cache Management --------------------
    suspend fun invalidateProfile(userId: String) {
        dataStore.edit { preferences ->
            preferences.remove(profileKey(userId))
            preferences.remove(profileTimestampKey(userId))
            preferences.remove(collectionsKey(userId))
            preferences.remove(activityKey(userId))
        }
        Log.d(TAG, "Invalidated all cache for user: $userId")
    }

    suspend fun clearAllCache() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
        Log.d(TAG, "Cleared all profile cache")
    }

    private fun isCacheValid(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp < CACHE_EXPIRATION_MILLIS
    }
}
