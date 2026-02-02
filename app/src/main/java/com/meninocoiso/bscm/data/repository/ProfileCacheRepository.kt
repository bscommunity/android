package com.meninocoiso.bscm.data.repository

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.meninocoiso.bscm.data.remote.dto.activity.ActivityEntry
import com.meninocoiso.bscm.data.remote.dto.user.UserProfileResponse
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Collection
import kotlinx.coroutines.flow.first
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ProfileCacheRepository"
private const val CACHE_EXPIRATION_MILLIS = 5 * 60 * 1000L // 5 minutes

@Singleton
class ProfileCacheRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    companion object ProfileCacheKeys {
        // Owner profile cache keys
        val MY_PROFILE = stringPreferencesKey("my_profile")
        val MY_PROFILE_TIMESTAMP = longPreferencesKey("my_profile_timestamp")
        val MY_LIKES = stringPreferencesKey("my_likes")
        val MY_LIKES_TIMESTAMP = longPreferencesKey("my_likes_timestamp")
        val MY_BOOKMARKS = stringPreferencesKey("my_bookmarks")
        val MY_BOOKMARKS_TIMESTAMP = longPreferencesKey("my_bookmarks_timestamp")
        val MY_COLLECTIONS = stringPreferencesKey("my_collections")
        val MY_COLLECTIONS_TIMESTAMP = longPreferencesKey("my_collections_timestamp")
        val MY_ACTIVITY = stringPreferencesKey("my_activity")
        val MY_ACTIVITY_TIMESTAMP = longPreferencesKey("my_activity_timestamp")

        // Other user profile cache keys (using user ID as suffix)
        fun profileKey(userId: String) = stringPreferencesKey("profile_$userId")
        fun profileTimestampKey(userId: String) = longPreferencesKey("profile_timestamp_$userId")
        fun activityKey(userId: String) = stringPreferencesKey("activity_$userId")
        fun activityTimestampKey(userId: String) = longPreferencesKey("activity_timestamp_$userId")
        fun libraryKey(userId: String) = stringPreferencesKey("library_$userId")
        fun libraryTimestampKey(userId: String) = longPreferencesKey("library_timestamp_$userId")
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // -------------------- My Profile --------------------
    suspend fun cacheMyProfile(profile: UserProfileResponse) {
        try {
            val encoded = json.encodeToString(UserProfileResponse.serializer(), profile)
            dataStore.edit { preferences ->
                preferences[MY_PROFILE] = encoded
                preferences[MY_PROFILE_TIMESTAMP] = System.currentTimeMillis()
            }
            Log.d(TAG, "Cached my profile")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching my profile", e)
        }
    }

    suspend fun getMyProfile(): UserProfileResponse? {
        return try {
            val preferences = dataStore.data.first()
            val timestamp = preferences[MY_PROFILE_TIMESTAMP] ?: 0L

            if (!isCacheValid(timestamp)) {
                Log.d(TAG, "My profile cache expired")
                return null
            }

            val encoded = preferences[MY_PROFILE] ?: return null
            json.decodeFromString(UserProfileResponse.serializer(), encoded)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading my profile cache", e)
            null
        }
    }

    // -------------------- My Likes --------------------
    suspend fun cacheMyLikes(likes: List<Chart>) {
        try {
            val encoded = json.encodeToString(ListSerializer(Chart.serializer()), likes)
            dataStore.edit { preferences ->
                preferences[MY_LIKES] = encoded
                preferences[MY_LIKES_TIMESTAMP] = System.currentTimeMillis()
            }
            Log.d(TAG, "Cached ${likes.size} likes")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching likes", e)
        }
    }

    suspend fun getMyLikes(): List<Chart>? {
        return try {
            val preferences = dataStore.data.first()
            val timestamp = preferences[MY_LIKES_TIMESTAMP] ?: 0L

            if (!isCacheValid(timestamp)) {
                Log.d(TAG, "My likes cache expired")
                return null
            }

            val encoded = preferences[MY_LIKES] ?: return null
            json.decodeFromString(ListSerializer(Chart.serializer()), encoded)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading likes cache", e)
            null
        }
    }

    suspend fun addLikeToCache(chart: Chart) {
        try {
            val currentLikes = getMyLikes()?.toMutableList() ?: mutableListOf()
            if (currentLikes.none { it.id == chart.id }) {
                currentLikes.add(0, chart) // Add to beginning
                cacheMyLikes(currentLikes)
                Log.d(TAG, "Added like to cache: ${chart.id}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding like to cache", e)
        }
    }

    suspend fun removeLikeFromCache(contentId: String) {
        try {
            val currentLikes = getMyLikes()?.toMutableList() ?: return
            val removed = currentLikes.removeAll { it.id == contentId || it.contentId == contentId }
            if (removed) {
                cacheMyLikes(currentLikes)
                Log.d(TAG, "Removed like from cache: $contentId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing like from cache", e)
        }
    }

    // -------------------- My Bookmarks --------------------
    suspend fun cacheMyBookmarks(bookmarks: List<Chart>) {
        try {
            val encoded = json.encodeToString(ListSerializer(Chart.serializer()), bookmarks)
            dataStore.edit { preferences ->
                preferences[MY_BOOKMARKS] = encoded
                preferences[MY_BOOKMARKS_TIMESTAMP] = System.currentTimeMillis()
            }
            Log.d(TAG, "Cached ${bookmarks.size} bookmarks")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching bookmarks", e)
        }
    }

    suspend fun getMyBookmarks(): List<Chart>? {
        return try {
            val preferences = dataStore.data.first()
            val timestamp = preferences[MY_BOOKMARKS_TIMESTAMP] ?: 0L

            if (!isCacheValid(timestamp)) {
                Log.d(TAG, "My bookmarks cache expired")
                return null
            }

            val encoded = preferences[MY_BOOKMARKS] ?: return null
            json.decodeFromString(ListSerializer(Chart.serializer()), encoded)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading bookmarks cache", e)
            null
        }
    }

    suspend fun addBookmarkToCache(chart: Chart) {
        try {
            val currentBookmarks = getMyBookmarks()?.toMutableList() ?: mutableListOf()
            if (currentBookmarks.none { it.id == chart.id }) {
                currentBookmarks.add(0, chart) // Add to beginning
                cacheMyBookmarks(currentBookmarks)
                Log.d(TAG, "Added bookmark to cache: ${chart.id}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding bookmark to cache", e)
        }
    }

    suspend fun removeBookmarkFromCache(contentId: String) {
        try {
            val currentBookmarks = getMyBookmarks()?.toMutableList() ?: return
            val removed = currentBookmarks.removeAll { it.id == contentId || it.contentId == contentId }
            if (removed) {
                cacheMyBookmarks(currentBookmarks)
                Log.d(TAG, "Removed bookmark from cache: $contentId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing bookmark from cache", e)
        }
    }

    // -------------------- My Collections --------------------
    suspend fun cacheMyCollections(collections: List<Collection>) {
        try {
            val encoded = json.encodeToString(ListSerializer(Collection.serializer()), collections)
            dataStore.edit { preferences ->
                preferences[MY_COLLECTIONS] = encoded
                preferences[MY_COLLECTIONS_TIMESTAMP] = System.currentTimeMillis()
            }
            Log.d(TAG, "Cached ${collections.size} collections")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching collections", e)
        }
    }

    suspend fun getMyCollections(): List<Collection>? {
        return try {
            val preferences = dataStore.data.first()
            val timestamp = preferences[MY_COLLECTIONS_TIMESTAMP] ?: 0L

            if (!isCacheValid(timestamp)) {
                Log.d(TAG, "My collections cache expired")
                return null
            }

            val encoded = preferences[MY_COLLECTIONS] ?: return null
            json.decodeFromString(ListSerializer(Collection.serializer()), encoded)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading collections cache", e)
            null
        }
    }

    suspend fun addItemToCollectionCache(collectionId: String, chart: Chart) {
        try {
            val collections = getMyCollections()?.toMutableList() ?: return
            val collectionIndex = collections.indexOfFirst { it.id == collectionId }
            if (collectionIndex != -1) {
                val collection = collections[collectionIndex]
                val items = collection.items.toMutableList()
                if (items.none { it.id == chart.id }) {
                    items.add(0, chart)
                    collections[collectionIndex] = collection.copy(
                        items = items,
                        itemCount = items.size
                    )
                    cacheMyCollections(collections)
                    Log.d(TAG, "Added item to collection cache: $collectionId")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding item to collection cache", e)
        }
    }

    suspend fun removeItemFromCollectionCache(collectionId: String, contentId: String) {
        try {
            val collections = getMyCollections()?.toMutableList() ?: return
            val collectionIndex = collections.indexOfFirst { it.id == collectionId }
            if (collectionIndex != -1) {
                val collection = collections[collectionIndex]
                val items = collection.items.toMutableList()
                val removed = items.removeAll { it.id == contentId || (it as? Chart)?.contentId == contentId }
                if (removed) {
                    collections[collectionIndex] = collection.copy(
                        items = items,
                        itemCount = items.size
                    )
                    cacheMyCollections(collections)
                    Log.d(TAG, "Removed item from collection cache: $collectionId")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing item from collection cache", e)
        }
    }

    // -------------------- My Activity --------------------
    suspend fun cacheMyActivity(activity: List<ActivityEntry>) {
        try {
            val encoded = json.encodeToString(ListSerializer(ActivityEntry.serializer()), activity)
            dataStore.edit { preferences ->
                preferences[MY_ACTIVITY] = encoded
                preferences[MY_ACTIVITY_TIMESTAMP] = System.currentTimeMillis()
            }
            Log.d(TAG, "Cached ${activity.size} activity entries")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching activity", e)
        }
    }

    suspend fun getMyActivity(): List<ActivityEntry>? {
        return try {
            val preferences = dataStore.data.first()
            val timestamp = preferences[MY_ACTIVITY_TIMESTAMP] ?: 0L

            if (!isCacheValid(timestamp)) {
                Log.d(TAG, "My activity cache expired")
                return null
            }

            val encoded = preferences[MY_ACTIVITY] ?: return null
            json.decodeFromString(ListSerializer(ActivityEntry.serializer()), encoded)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading activity cache", e)
            null
        }
    }

    // -------------------- User Profile --------------------
    suspend fun cacheUserProfile(userId: String, profile: UserProfileResponse) {
        try {
            val encoded = json.encodeToString(UserProfileResponse.serializer(), profile)
            dataStore.edit { preferences ->
                preferences[profileKey(userId)] = encoded
                preferences[profileTimestampKey(userId)] = System.currentTimeMillis()
            }
            Log.d(TAG, "Cached profile for user: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching user profile", e)
        }
    }

    suspend fun getUserProfile(userId: String): UserProfileResponse? {
        return try {
            val preferences = dataStore.data.first()
            val timestamp = preferences[profileTimestampKey(userId)] ?: 0L

            if (!isCacheValid(timestamp)) {
                Log.d(TAG, "User profile cache expired for: $userId")
                return null
            }

            val encoded = preferences[profileKey(userId)] ?: return null
            json.decodeFromString(UserProfileResponse.serializer(), encoded)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading user profile cache", e)
            null
        }
    }

    // -------------------- User Activity --------------------
    suspend fun cacheUserActivity(userId: String, activity: List<ActivityEntry>) {
        try {
            val encoded = json.encodeToString(ListSerializer(ActivityEntry.serializer()), activity)
            dataStore.edit { preferences ->
                preferences[activityKey(userId)] = encoded
                preferences[activityTimestampKey(userId)] = System.currentTimeMillis()
            }
            Log.d(TAG, "Cached ${activity.size} activity entries for user: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching user activity", e)
        }
    }

    suspend fun getUserActivity(userId: String): List<ActivityEntry>? {
        return try {
            val preferences = dataStore.data.first()
            val timestamp = preferences[activityTimestampKey(userId)] ?: 0L

            if (!isCacheValid(timestamp)) {
                Log.d(TAG, "User activity cache expired for: $userId")
                return null
            }

            val encoded = preferences[activityKey(userId)] ?: return null
            json.decodeFromString(ListSerializer(ActivityEntry.serializer()), encoded)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading user activity cache", e)
            null
        }
    }

    // -------------------- User Library --------------------
    suspend fun cacheUserLibrary(userId: String, library: List<Chart>) {
        try {
            val encoded = json.encodeToString(ListSerializer(Chart.serializer()), library)
            dataStore.edit { preferences ->
                preferences[libraryKey(userId)] = encoded
                preferences[libraryTimestampKey(userId)] = System.currentTimeMillis()
            }
            Log.d(TAG, "Cached ${library.size} library items for user: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error caching user library", e)
        }
    }

    suspend fun getUserLibrary(userId: String): List<Chart>? {
        return try {
            val preferences = dataStore.data.first()
            val timestamp = preferences[libraryTimestampKey(userId)] ?: 0L

            if (!isCacheValid(timestamp)) {
                Log.d(TAG, "User library cache expired for: $userId")
                return null
            }

            val encoded = preferences[libraryKey(userId)] ?: return null
            json.decodeFromString(ListSerializer(Chart.serializer()), encoded)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading user library cache", e)
            null
        }
    }

    // -------------------- Cache Management --------------------
    suspend fun invalidateMyProfile() {
        dataStore.edit { preferences ->
            preferences.remove(MY_PROFILE)
            preferences.remove(MY_PROFILE_TIMESTAMP)
        }
        Log.d(TAG, "Invalidated my profile cache")
    }

    suspend fun invalidateMyLikes() {
        dataStore.edit { preferences ->
            preferences.remove(MY_LIKES)
            preferences.remove(MY_LIKES_TIMESTAMP)
        }
        Log.d(TAG, "Invalidated my likes cache")
    }

    suspend fun invalidateMyBookmarks() {
        dataStore.edit { preferences ->
            preferences.remove(MY_BOOKMARKS)
            preferences.remove(MY_BOOKMARKS_TIMESTAMP)
        }
        Log.d(TAG, "Invalidated my bookmarks cache")
    }

    suspend fun invalidateMyCollections() {
        dataStore.edit { preferences ->
            preferences.remove(MY_COLLECTIONS)
            preferences.remove(MY_COLLECTIONS_TIMESTAMP)
        }
        Log.d(TAG, "Invalidated my collections cache")
    }

    suspend fun invalidateUserProfile(userId: String) {
        dataStore.edit { preferences ->
            preferences.remove(profileKey(userId))
            preferences.remove(profileTimestampKey(userId))
        }
        Log.d(TAG, "Invalidated profile cache for user: $userId")
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
