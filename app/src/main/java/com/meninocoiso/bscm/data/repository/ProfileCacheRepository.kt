package com.meninocoiso.bscm.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.meninocoiso.bscm.data.remote.dto.activity.ActivityItemResponse
import com.meninocoiso.bscm.data.remote.dto.user.UserProfileResponse
import com.meninocoiso.bscm.presentation.viewmodel.profile.PagedResult
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ProfileCacheRepository"
private const val QUICK_CACHE_EXPIRATION_MILLIS = 10 * 60 * 1000L // 10 minutes
private const val OWNER_ID = "owner"

@Singleton
class ProfileCacheRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    companion object ProfileCacheKeys {
        fun profileKey(userId: String) = stringPreferencesKey("profile_$userId")
        fun profileTimestampKey(userId: String) = longPreferencesKey("profile_timestamp_$userId")

        // Sections
        fun activityKey(userId: String) = stringPreferencesKey("activity_$userId")
        fun libraryIdsKey(userId: String) = stringPreferencesKey("library_ids_$userId")
        fun collectionsIdsKey(userId: String) = stringPreferencesKey("collections_ids_$userId")

        // Counts
        fun libraryCountKey(userId: String) = longPreferencesKey("library_count_$userId")
        fun collectionsCountKey(userId: String) = longPreferencesKey("collections_count_$userId")
        val likesCountKey = longPreferencesKey("likes_count")
        val bookmarksCountKey = longPreferencesKey("bookmarks_count")
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

    suspend fun clearProfile(userId: String) {
        dataStore.edit { preferences ->
            preferences.remove(profileKey(userId))
            preferences.remove(profileTimestampKey(userId))
        }
        Log.d(TAG, "Cleared profile cache for user: $userId")
    }

    // -------------------- Collections IDs --------------------
    suspend fun cacheCollectionIds(userId: String, collectionIds: List<String>, total: Long?) {
        writeStringList(collectionsIdsKey(userId), collectionIds)
        total?.let {
            dataStore.edit { preferences ->
                preferences[collectionsCountKey(userId)] = it
            }
        }
        touchProfileTimestamp(userId)
    }

    suspend fun getCollections(userId: String): PagedResult<String> {
        return try {
            val preferences = dataStore.data.first()
            val timestamp = preferences[profileTimestampKey(userId)] ?: 0L

            if (!isQuickCacheValid(timestamp)) {
                invalidateProfile(userId)
                PagedResult(emptyList(), null)
            } else {
                val items = readStringList(collectionsIdsKey(userId)) ?: emptyList()
                val total = preferences[collectionsCountKey(userId)]?.toInt()
                PagedResult(items, total)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading collections IDs cache for user: $userId", e)
            PagedResult(emptyList(), null)
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
            val timestamp = preferences[profileTimestampKey(userId)] ?: 0L
            if (!isQuickCacheValid(timestamp)) {
                invalidateActivity(userId)
                return null
            }
            val encoded = preferences[activityKey(userId)] ?: return null
            json.decodeFromString(ListSerializer(ActivityItemResponse.serializer()), encoded)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading activity cache for user: $userId", e)
            null
        }
    }

    suspend fun getActivity(): List<ActivityItemResponse>? = getActivity(OWNER_ID)

    // ------------------ Library IDs (content made by the user) --------------------
    suspend fun cacheLibrary(userId: String, chartIds: List<String>, total: Long? = null) {
        writeStringList(libraryIdsKey(userId), chartIds)
        total?.let {
            dataStore.edit { preferences ->
                preferences[libraryCountKey(userId)] = it
            }
        }
        touchProfileTimestamp(userId)
    }

    suspend fun getLibrary(userId: String): PagedResult<String>? {
        return try {
            val preferences = dataStore.data.first()
            val timestamp = preferences[profileTimestampKey(userId)] ?: 0L

            if (!isQuickCacheValid(timestamp)) {
                invalidateProfile(userId)
                null
            } else {
                val items = readStringList(libraryIdsKey(userId))
                val total = preferences[libraryCountKey(userId)]?.toInt()

                items?.let { PagedResult(it, total) }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading library IDs cache for user: $userId", e)
            null
        }
    }

    // ------------------- Likes and Bookmarks counts (used in profile header) --------------------
    suspend fun cacheLikesCount(count: Long) {
        dataStore.edit { preferences ->
            preferences[likesCountKey] = count
        }
    }

    suspend fun getLikesCount(): Long? {
        return try {
            val preferences = dataStore.data.first()
            Log.d(TAG, "Read likes count from cache: ${preferences[likesCountKey]}")
            preferences[likesCountKey]
        } catch (e: Exception) {
            Log.e(TAG, "Error reading likes count from cache", e)
            null
        }
    }

    suspend fun cacheBookmarksCount(count: Long) {
        dataStore.edit { preferences ->
            preferences[bookmarksCountKey] = count
        }
    }

    suspend fun getBookmarksCount(): Long? {
        return try {
            val preferences = dataStore.data.first()
            preferences[bookmarksCountKey]
        } catch (e: Exception) {
            Log.e(TAG, "Error reading bookmarks count from cache", e)
            null
        }
    }

    // -------------------- Cache Management --------------------
    suspend fun invalidateActivity(userId: String) {
        dataStore.edit { preferences ->
            preferences.remove(activityKey(userId))
            preferences.remove(profileTimestampKey(userId))
        }
    }

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
