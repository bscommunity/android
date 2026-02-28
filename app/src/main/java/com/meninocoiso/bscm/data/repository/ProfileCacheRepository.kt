package com.meninocoiso.bscm.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.meninocoiso.bscm.data.remote.dto.activity.ActivityItemResponse
import com.meninocoiso.bscm.data.remote.dto.user.UserProfileResponse
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ProfileCacheRepository"
private const val QUICK_CACHE_EXPIRATION_MILLIS = 10 * 60 * 1000L // 10 minutes for other profiles
private const val OWNER_ID = "owner"

@Singleton
class ProfileCacheRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    companion object ProfileCacheKeys {
        fun profileKey(userId: String) = stringPreferencesKey("profile_$userId")
        fun profileTimestampKey(userId: String) = longPreferencesKey("profile_timestamp_$userId")

        fun collectionsIdsKey(userId: String) = stringPreferencesKey("collections_ids_$userId")
        fun activityKey(userId: String) = stringPreferencesKey("activity_$userId")
        fun libraryIdsKey(userId: String) = stringPreferencesKey("library_ids_$userId")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private suspend fun writeStringList(key: Preferences.Key<String>, values: List<String>) {
        val encoded = json.encodeToString(ListSerializer(String.serializer()), values)
        dataStore.edit { preferences ->
            preferences[key] = encoded
        }
    }

    private suspend fun readStringList(key: Preferences.Key<String>): List<String>? {
        val preferences = dataStore.data.first()
        val encoded = preferences[key] ?: return null
        return json.decodeFromString(ListSerializer(String.serializer()), encoded)
    }

    private suspend fun touchProfileTimestamp(userId: String) {
        dataStore.edit { preferences ->
            preferences[profileTimestampKey(userId)] = System.currentTimeMillis()
        }
    }

    // -------------------- Profile Header --------------------
    suspend fun cacheProfile(username: String, profile: UserProfileResponse) {
        try {
            val encoded = json.encodeToString(UserProfileResponse.serializer(), profile)
            dataStore.edit { preferences ->
                preferences[profileKey(username)] = encoded
                preferences[profileTimestampKey(username)] = System.currentTimeMillis()
            }
            Log.d(TAG, "Cached profile header for user: $username")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching profile header for user: $username", e)
        }
    }

    suspend fun cacheProfile(profile: UserProfileResponse) = cacheProfile(OWNER_ID, profile)

    suspend fun getProfile(username: String): UserProfileResponse? {
        return try {
            val preferences = dataStore.data.first()
            val timestamp = preferences[profileTimestampKey(username)] ?: 0L

            if (!isQuickCacheValid(timestamp)) {
                invalidateProfile(username)
                return null
            }

            val encoded = preferences[profileKey(username)] ?: return null
            json.decodeFromString(UserProfileResponse.serializer(), encoded)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading profile header cache for user: $username", e)
            null
        }
    }

    suspend fun getProfile(): UserProfileResponse? {
        return try {
            val preferences = dataStore.data.first()
            val encoded = preferences[profileKey(OWNER_ID)] ?: return null
            json.decodeFromString(UserProfileResponse.serializer(), encoded)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading owner profile cache", e)
            null
        }
    }

    suspend fun clearProfile(userId: String) {
        dataStore.edit { preferences ->
            preferences.remove(profileKey(userId))
            preferences.remove(profileTimestampKey(userId))
        }
        Log.d(TAG, "Cleared profile cache for user: $userId")
    }

    suspend fun clearProfile() = clearProfile(OWNER_ID)

    // -------------------- Collections IDs --------------------
    suspend fun cacheCollectionIds(userId: String, collectionIds: List<String>) {
        writeStringList(collectionsIdsKey(userId), collectionIds)
        touchProfileTimestamp(userId)
    }

    suspend fun getCollections(userId: String): List<String>? {
        return try {
            val preferences = dataStore.data.first()
            val timestamp = preferences[profileTimestampKey(userId)] ?: 0L
            if (!isQuickCacheValid(timestamp)) {
                invalidateProfile(userId)
                return null
            }
            readStringList(collectionsIdsKey(userId))
        } catch (e: Exception) {
            Log.e(TAG, "Error reading collections IDs cache for user: $userId", e)
            null
        }
    }

    // -------------------- Activity (quick cache for other profiles) --------------------
    suspend fun cacheActivity(userId: String, activity: List<ActivityItemResponse>) {
        try {
            val encoded = json.encodeToString(ListSerializer(ActivityItemResponse.serializer()), activity)
            dataStore.edit { preferences ->
                preferences[activityKey(userId)] = encoded
                preferences[profileTimestampKey(userId)] = System.currentTimeMillis()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error caching activity for user: $userId", e)
        }
    }

    suspend fun cacheActivity(activity: List<ActivityItemResponse>) = cacheActivity(OWNER_ID, activity)

    suspend fun getActivity(userId: String): List<ActivityItemResponse>? {
        return try {
            val preferences = dataStore.data.first()
            if (userId != OWNER_ID && userId != "user") {
                val timestamp = preferences[profileTimestampKey(userId)] ?: 0L
                if (!isQuickCacheValid(timestamp)) {
                    invalidateProfile(userId)
                    return null
                }
            }
            val encoded = preferences[activityKey(userId)] ?: return null
            json.decodeFromString(ListSerializer(ActivityItemResponse.serializer()), encoded)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading activity cache for user: $userId", e)
            null
        }
    }

    suspend fun getActivity(): List<ActivityItemResponse>? = getActivity(OWNER_ID)

    // -------------------- Library IDs (charts from other profiles) --------------------
    suspend fun cacheLibrary(userId: String, chartIds: List<String>) {
        writeStringList(libraryIdsKey(userId), chartIds)
        if (userId != OWNER_ID && userId != "user") {
            touchProfileTimestamp(userId)
        }
    }

    suspend fun getLibrary(userId: String): List<String>? {
        return try {
            if (userId != OWNER_ID && userId != "user") {
                val preferences = dataStore.data.first()
                val timestamp = preferences[profileTimestampKey(userId)] ?: 0L
                if (!isQuickCacheValid(timestamp)) {
                    invalidateProfile(userId)
                    return null
                }
            }
            readStringList(libraryIdsKey(userId))
        } catch (e: Exception) {
            Log.e(TAG, "Error reading library IDs cache for user: $userId", e)
            null
        }
    }

    // -------------------- Cache Management --------------------
    suspend fun invalidateProfile(userId: String) {
        dataStore.edit { preferences ->
            preferences.remove(profileKey(userId))
            preferences.remove(profileTimestampKey(userId))
            preferences.remove(collectionsIdsKey(userId))
            preferences.remove(activityKey(userId))
            preferences.remove(libraryIdsKey(userId))
        }
    }

    private fun isQuickCacheValid(timestamp: Long): Boolean {
        return System.currentTimeMillis() - timestamp < QUICK_CACHE_EXPIRATION_MILLIS
    }
}
