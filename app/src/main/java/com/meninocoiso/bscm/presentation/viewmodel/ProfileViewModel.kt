package com.meninocoiso.bscm.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.data.remote.dto.activity.ActivityEntry
import com.meninocoiso.bscm.data.remote.dto.user.UserProfileResponse
import com.meninocoiso.bscm.domain.enums.CollectionKind
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Collection
import com.meninocoiso.bscm.domain.repository.CollectionRepository
import com.meninocoiso.bscm.domain.repository.MeRepository
import com.meninocoiso.bscm.domain.repository.ProfileRepository
import com.meninocoiso.bscm.domain.result.ContentState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.util.Date
import javax.inject.Inject

private const val TAG = "ProfileViewModel"

data class ActivityItem(val date: Date, val content: List<CatalogItem>)

data class ProfilePaginationState(
    val isLoadingMoreActivity: Boolean = false,
    val hasMoreActivity: Boolean = false,
    val isLoadingMoreLibrary: Boolean = false,
    val hasMoreLibrary: Boolean = false,
    val isLoadingMoreLikes: Boolean = false,
    val hasMoreLikes: Boolean = false,
    val isLoadingMoreBookmarks: Boolean = false,
    val hasMoreBookmarks: Boolean = false,
    val isLoadingMoreCollections: Boolean = false,
    val hasMoreCollections: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val meRepository: MeRepository,
    private val collectionRepository: CollectionRepository,
    private val apiClient: ApiClient
) : ViewModel() {
    private val activityPagination = PaginationState(pageSize = 20)
    private val libraryPagination = PaginationState(pageSize = 20)
    private val likesPagination = PaginationState(pageSize = 20)
    private val bookmarksPagination = PaginationState(pageSize = 20)
    private val collectionsPagination = PaginationState(pageSize = 20)

    private val _isLoadingMoreActivity = MutableStateFlow(false)
    val isLoadingMoreActivity: StateFlow<Boolean> = _isLoadingMoreActivity.asStateFlow()
    private val _hasMoreActivity = MutableStateFlow(ProfilePaginationState().hasMoreActivity)
    val hasMoreActivity: StateFlow<Boolean> = _hasMoreActivity.asStateFlow()

    private val _isLoadingMoreLibrary = MutableStateFlow(false)
    val isLoadingMoreLibrary: StateFlow<Boolean> = _isLoadingMoreLibrary.asStateFlow()
    private val _hasMoreLibrary = MutableStateFlow(ProfilePaginationState().hasMoreLibrary)
    val hasMoreLibrary: StateFlow<Boolean> = _hasMoreLibrary.asStateFlow()

    private val _isLoadingMoreLikes = MutableStateFlow(false)
    val isLoadingMoreLikes: StateFlow<Boolean> = _isLoadingMoreLikes.asStateFlow()
    private val _hasMoreLikes = MutableStateFlow(ProfilePaginationState().hasMoreLikes)
    val hasMoreLikes: StateFlow<Boolean> = _hasMoreLikes.asStateFlow()

    private val _isLoadingMoreBookmarks = MutableStateFlow(false)
    val isLoadingMoreBookmarks: StateFlow<Boolean> = _isLoadingMoreBookmarks.asStateFlow()
    private val _hasMoreBookmarks = MutableStateFlow(ProfilePaginationState().hasMoreBookmarks)
    val hasMoreBookmarks: StateFlow<Boolean> = _hasMoreBookmarks.asStateFlow()

    private val _isLoadingMoreCollections = MutableStateFlow(false)
    val isLoadingMoreCollections: StateFlow<Boolean> = _isLoadingMoreCollections.asStateFlow()
    private val _hasMoreCollections = MutableStateFlow(ProfilePaginationState().hasMoreCollections)
    val hasMoreCollections: StateFlow<Boolean> = _hasMoreCollections.asStateFlow()

    val paginationState: StateFlow<ProfilePaginationState> = combine(
        isLoadingMoreActivity,
        hasMoreActivity,
        isLoadingMoreLibrary,
        hasMoreLibrary,
        isLoadingMoreLikes,
        hasMoreLikes,
        isLoadingMoreBookmarks,
        hasMoreBookmarks,
        isLoadingMoreCollections,
        hasMoreCollections
    ) { values ->
        ProfilePaginationState(
            isLoadingMoreActivity = values[0],
            hasMoreActivity = values[1],
            isLoadingMoreLibrary = values[2],
            hasMoreLibrary = values[3],
            isLoadingMoreLikes = values[4],
            hasMoreLikes = values[5],
            isLoadingMoreBookmarks = values[6],
            hasMoreBookmarks = values[7],
            isLoadingMoreCollections = values[8],
            hasMoreCollections = values[9]
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ProfilePaginationState()
    )

    private val _section1State = MutableStateFlow<ContentState>(ContentState.Loading)
    val section1State: SharedFlow<ContentState> = _section1State.asStateFlow()
    private val _section2State = MutableStateFlow<ContentState>(ContentState.Loading)
    val section2State: SharedFlow<ContentState> = _section2State.asStateFlow()

    private val _activityContent = MutableStateFlow<List<ActivityItem>>(emptyList())
    val activityContent: StateFlow<List<ActivityItem>> = _activityContent.asStateFlow()

    private val _libraryContent = MutableStateFlow<List<CatalogItem>>(emptyList())
    val libraryContent: StateFlow<List<CatalogItem>> = _libraryContent.asStateFlow()

    private val _likedContent = MutableStateFlow<List<CatalogItem>>(emptyList())
    val likedContent: StateFlow<List<CatalogItem>> = _likedContent.asStateFlow()

    private val _collectionContent = MutableStateFlow<List<Collection>>(emptyList())
    val collectionContent: StateFlow<List<Collection>> = _collectionContent.asStateFlow()

    private val _isFollowing = MutableStateFlow(false)
    val isFollowing: StateFlow<Boolean> = _isFollowing.asStateFlow()

    private var currentProfileId: String? = null
    private var isOwnerProfile: Boolean = false
    private var profileHeader: UserProfileResponse? = null

    /**
     * Loads the profile for the given user ID. If it's the owner's profile, fetches from MeRepository,
     * otherwise from ProfileRepository. Resets all states if the profile changes.
     *
     * @param userId The ID of the user whose profile to load.
     * @param isOwner Whether the profile belongs to the current user.
     */
    fun loadProfile(userId: String, isOwner: Boolean) {
        if (currentProfileId == userId && isOwnerProfile == isOwner) return

        currentProfileId = userId
        isOwnerProfile = isOwner

        resetAll()

        viewModelScope.launch {
            if (isOwner) {
                Log.d(TAG, "Fetching owner profile")
                meRepository.getProfile().onSuccess { header ->
                    profileHeader = header
                    _isFollowing.value = false
                    Log.d(TAG, "Owner profile loaded successfully for userId: $userId")
                }.onFailure {
                    Log.e(TAG, "Error loading owner profile for userId: $userId", it)
                }
            } else {
                Log.d(TAG, "Fetching profile header for userId: $userId")
                profileRepository.getProfileHeader(userId).onSuccess { header ->
                    profileHeader = header
                    _isFollowing.value = header.isFollowing ?: false
                    Log.d(TAG, "Profile header loaded successfully for userId: $userId")
                }.onFailure {
                    Log.e(TAG, "Error loading profile header for userId: $userId", it)
                }
            }
        }
    }

    /**
     * Handles tab selection for the profile view. Depending on whether it's the owner's profile,
     * fetches the appropriate content for the selected tab if not already loaded.
     *
     * @param userId The ID of the user.
     * @param isOwner Whether the profile belongs to the current user.
     * @param index The index of the selected tab (0 for likes/activity, 1 for collections/library).
     */
    fun onTabSelected(userId: String, isOwner: Boolean, index: Int) {
        // Log.d(TAG, "Tab selected: index=$index, isOwner=$isOwner")
        if (isOwner) {
            when (index) {
                0 -> if (_likedContent.value.isEmpty()) fetchUserLikes(reset = true)
                1 -> if (_collectionContent.value.isEmpty()) fetchUserCollections(reset = true)
            }
        } else {
            when (index) {
                0 -> if (_activityContent.value.isEmpty()) fetchProfileActivity(
                    userId,
                    reset = true
                )

                1 -> if (_libraryContent.value.isEmpty()) fetchProfileLibrary(userId, reset = true)
            }
        }
    }

    /**
     * Toggles the follow status for the given user. If not the owner, follows or unfollows the user.
     *
     * @param userId The ID of the user to follow or unfollow.
     */
    fun toggleFollow(userId: String) {
        if (isOwnerProfile) return

        viewModelScope.launch {
            val shouldFollow = !_isFollowing.value
            val result = if (shouldFollow) {
                Log.d(TAG, "Following user: $userId")
                profileRepository.followUser(userId)
            } else {
                Log.d(TAG, "Unfollowing user: $userId")
                profileRepository.unfollowUser(userId)
            }
            result.onSuccess {
                _isFollowing.value = shouldFollow
                Log.d(TAG, "Follow status toggled to $shouldFollow for userId: $userId")
            }.onFailure {
                Log.e(TAG, "Error toggling follow for userId: $userId", it)
            }
        }
    }

    /**
     * Fetches the activity for the given user ID with pagination support.
     *
     * @param userId The ID of the user whose activity to fetch.
     * @param reset Whether to reset the pagination and content.
     */
    fun fetchProfileActivity(userId: String, reset: Boolean = false) {
        viewModelScope.launch {
            if (reset) {
                activityPagination.reset()
                _activityContent.value = emptyList()
                _section1State.value = ContentState.Loading
            }

            if (activityPagination.isLoadingMore || !activityPagination.hasMore) return@launch

            activityPagination.isLoadingMore = true
            _isLoadingMoreActivity.value = true
            Log.d(
                TAG,
                "Fetching activity for userId: $userId, limit: ${activityPagination.pageSize}, offset: ${activityPagination.nextOffset()}"
            )
            val result = profileRepository.getActivity(
                userId = userId,
                limit = activityPagination.pageSize,
                offset = activityPagination.nextOffset()
            )

            result.onSuccess { entries ->
                val items = mapActivityEntries(entries)
                val updated =
                    if (activityPagination.currentPage == 0) items else _activityContent.value + items
                _activityContent.value = updated
                activityPagination.hasMore = entries.size >= activityPagination.pageSize
                _hasMoreActivity.value = activityPagination.hasMore
                activityPagination.advancePage()
                _section1State.value = ContentState.Success
                Log.d(
                    TAG,
                    "Fetched activity for userId: $userId, entries: ${entries.size}, total: ${updated.size}"
                )
            }.onFailure {
                _section1State.value = ContentState.Error
                activityPagination.hasMore = false
                _hasMoreActivity.value = false
                Log.e(TAG, "Error fetching activity for userId: $userId", it)
            }

            activityPagination.isLoadingMore = false
            _isLoadingMoreActivity.value = false
        }
    }

    /**
     * Fetches the library (charts) for the given user ID with pagination support.
     *
     * @param userId The ID of the user whose library to fetch.
     * @param reset Whether to reset the pagination and content.
     */
    fun fetchProfileLibrary(userId: String, reset: Boolean = false) {
        viewModelScope.launch {
            if (reset) {
                libraryPagination.reset()
                _libraryContent.value = emptyList()
                _section2State.value = ContentState.Loading
            }

            if (libraryPagination.isLoadingMore || !libraryPagination.hasMore) return@launch

            libraryPagination.isLoadingMore = true
            _isLoadingMoreLibrary.value = true

            Log.d(
                TAG,
                "Fetching library for userId: $userId, limit: ${libraryPagination.pageSize}, offset: ${libraryPagination.nextOffset()}"
            )
            val result = profileRepository.getUserCharts(
                userId = userId,
                limit = libraryPagination.pageSize,
                offset = libraryPagination.nextOffset()
            )

            result.onSuccess { data ->
                val updated =
                    if (libraryPagination.currentPage == 0) data else _libraryContent.value + data
                _libraryContent.value = updated
                libraryPagination.hasMore = data.size >= libraryPagination.pageSize
                _hasMoreLibrary.value = libraryPagination.hasMore
                libraryPagination.advancePage()
                _section2State.value = ContentState.Success
                Log.d(
                    TAG,
                    "Fetched library for userId: $userId, charts: ${data.size}, total: ${updated.size}"
                )
            }.onFailure {
                _section2State.value = ContentState.Error
                libraryPagination.hasMore = false
                _hasMoreLibrary.value = false
                Log.e(TAG, "Error fetching library for userId: $userId", it)
            }

            libraryPagination.isLoadingMore = false
            _isLoadingMoreLibrary.value = false
        }
    }

    /**
     * Fetches the user's liked content with pagination support.
     *
     * @param reset Whether to reset the pagination and content.
     */
    fun fetchUserLikes(reset: Boolean = false) {
        viewModelScope.launch {
            if (reset) {
                likesPagination.reset()
                _likedContent.value = emptyList()
                _section1State.value = ContentState.Loading
            }

            if (likesPagination.isLoadingMore || !likesPagination.hasMore) return@launch

            likesPagination.isLoadingMore = true
            _isLoadingMoreLikes.value = true

            Log.d(
                TAG,
                "Fetching likes, limit: ${likesPagination.pageSize}, offset: ${likesPagination.nextOffset()}"
            )
            val result = meRepository.getLikes(
                limit = likesPagination.pageSize,
                offset = likesPagination.nextOffset()
            )

            result.onSuccess { data ->
                val updated =
                    if (likesPagination.currentPage == 0) data else _likedContent.value + data
                _likedContent.value = updated
                likesPagination.hasMore = data.size >= likesPagination.pageSize
                _hasMoreLikes.value = likesPagination.hasMore
                likesPagination.advancePage()
                _section1State.value = ContentState.Success

                Log.d(TAG, "Fetched likes: ${data.size}, total: ${updated.size}")
            }.onFailure {
                _section1State.value = ContentState.Error
                likesPagination.hasMore = false
                _hasMoreLikes.value = false
                Log.e(TAG, "Error fetching likes", it)
            }

            likesPagination.isLoadingMore = false
            _isLoadingMoreLikes.value = false
        }
    }

    /**
     * Fetches the user's collections (bookmarks and custom collections) with pagination support.
     *
     * @param reset Whether to reset the pagination and content.
     */
    fun fetchUserCollections(reset: Boolean = false) {
        viewModelScope.launch {
            if (reset) {
                bookmarksPagination.reset()
                collectionsPagination.reset()
                _collectionContent.value = emptyList()
                _section2State.value = ContentState.Loading
            }

            Log.d(TAG, "Fetching user collections, reset: $reset")
            fetchBookmarksInternal()
            fetchCustomCollectionsInternal()
        }
    }

    /**
     * Loads more activity for the given user ID.
     *
     * @param userId The ID of the user whose activity to load more.
     */
    fun loadMoreActivity(userId: String) = fetchProfileActivity(userId, reset = false)

    /**
     * Loads more library for the given user ID.
     *
     * @param userId The ID of the user whose library to load more.
     */
    fun loadMoreLibrary(userId: String) = fetchProfileLibrary(userId, reset = false)

    /**
     * Loads more likes for the current user.
     */
    fun loadMoreLikes() = fetchUserLikes(reset = false)

    /**
     * Loads more bookmarks for the current user.
     */
    fun loadMoreBookmarks() {
        Log.d(TAG, "Loading more bookmarks")
        viewModelScope.launch { fetchBookmarksInternal() }
    }

    /**
     * Loads more collections for the current user.
     */
    fun loadMoreCollections() {
        Log.d(TAG, "Loading more custom collections")
        viewModelScope.launch { fetchCustomCollectionsInternal() }
    }

    /**
     * Fetches bookmarks internally with pagination support.
     */
    private suspend fun fetchBookmarksInternal() {
        if (bookmarksPagination.isLoadingMore || !bookmarksPagination.hasMore) return

        bookmarksPagination.isLoadingMore = true
        _isLoadingMoreBookmarks.value = true
        Log.d(
            TAG,
            "Fetching bookmarks, limit: ${bookmarksPagination.pageSize}, offset: ${bookmarksPagination.nextOffset()}"
        )
        val result = meRepository.getBookmarks(
            limit = bookmarksPagination.pageSize,
            offset = bookmarksPagination.nextOffset()
        )

        result.onSuccess { data ->
            val updatedBookmarks = if (bookmarksPagination.currentPage == 0) data else {
                val existing = _collectionContent.value.firstOrNull { it.id == "bookmarks" }?.items
                    ?: emptyList()
                existing + data
            }

            Log.d("hasMore: ", "${data.size} >= ${bookmarksPagination.pageSize} - dataSize: ${data.size}")
            bookmarksPagination.hasMore = data.size >= bookmarksPagination.pageSize
            _hasMoreBookmarks.value = bookmarksPagination.hasMore
            bookmarksPagination.advancePage()
            updateCollectionContent(updatedBookmarks = updatedBookmarks)
            _section2State.value = ContentState.Success
            Log.d(TAG, "Fetched bookmarks: ${data.size}, total: ${updatedBookmarks.size}")
        }.onFailure {
            _section2State.value = ContentState.Error
            bookmarksPagination.hasMore = false
            _hasMoreBookmarks.value = false
            Log.e(TAG, "Error fetching bookmarks", it)
        }

        bookmarksPagination.isLoadingMore = false
        _isLoadingMoreBookmarks.value = false
    }

    /**
     * Fetches custom collections internally with pagination support.
     */
    private suspend fun fetchCustomCollectionsInternal() {
        if (collectionsPagination.isLoadingMore || !collectionsPagination.hasMore) return

        collectionsPagination.isLoadingMore = true
        _isLoadingMoreCollections.value = true
        Log.d(
            TAG,
            "Fetching custom collections, limit: ${collectionsPagination.pageSize}, offset: ${collectionsPagination.nextOffset()}"
        )
        val result = collectionRepository.getUserCollections(
            limit = collectionsPagination.pageSize,
            offset = collectionsPagination.nextOffset()
        )

        result.onSuccess { data ->
            val existingCustom = _collectionContent.value.filter { it.id != "bookmarks" }
            val updatedCustom =
                if (collectionsPagination.currentPage == 0) data else existingCustom + data
            collectionsPagination.hasMore = data.size >= collectionsPagination.pageSize
            _hasMoreCollections.value = collectionsPagination.hasMore
            collectionsPagination.advancePage()
            updateCollectionContent(customCollections = updatedCustom)
            _section2State.value = ContentState.Success
            Log.d(TAG, "Fetched custom collections: ${data.size}, total: ${updatedCustom.size}")
        }.onFailure {
            _section2State.value = ContentState.Error
            collectionsPagination.hasMore = false
            _hasMoreCollections.value = false
            Log.e(TAG, "Error fetching custom collections", it)
        }

        collectionsPagination.isLoadingMore = false
        _isLoadingMoreCollections.value = false
    }

    /**
     * Updates the collection content by merging bookmarks and custom collections.
     *
     * @param updatedBookmarks The updated list of bookmarks.
     * @param customCollections The updated list of custom collections.
     */
    private fun updateCollectionContent(
        updatedBookmarks: List<CatalogItem>? = null,
        customCollections: List<Collection>? = null
    ) {
        val items = _collectionContent.value
        val existingBookmarks = items.find { it.kind == CollectionKind.BOOKMARKS }
        val existingCustom = items.filter { it.kind == CollectionKind.USER }

        if (updatedBookmarks == null && customCollections == null) return

        val bookmarksCollection: Collection? = when {
            updatedBookmarks != null && existingBookmarks != null -> existingBookmarks.copy(items = updatedBookmarks)
            else -> existingBookmarks
        }

        val customs = customCollections ?: existingCustom

        val merged = mutableListOf<Collection>()
        bookmarksCollection?.let { merged.add(it) }
        merged.addAll(customs)

        _collectionContent.value = merged
    }

    /**
     * Maps activity entries to activity items by fetching corresponding charts.
     *
     * @param entries The list of activity entries to map.
     * @return The list of activity items.
     */
    private suspend fun mapActivityEntries(entries: List<ActivityEntry>): List<ActivityItem> {
        val ids = entries.map { it.targetId }.distinct()
        val charts = if (ids.isNotEmpty()) {
            Log.d(TAG, "Fetching charts by IDs: $ids")
            runCatching { apiClient.getChartsById(ids) }.getOrDefault(emptyList())
        } else {
            emptyList()
        }
        val chartById = charts.associateBy { it.id }
        val chartByContentId = charts.associateBy { it.contentId ?: it.id }

        return entries.mapNotNull { entry ->
            val chart = chartById[entry.targetId] ?: chartByContentId[entry.targetId]
            chart?.let {
                ActivityItem(
                    date = Date.from(entry.createdAt.atZone(ZoneId.systemDefault()).toInstant()),
                    content = listOf(it)
                )
            }
        }
    }

    /**
     * Resets all pagination states and content to their initial values.
     */
    private fun resetAll() {
        activityPagination.reset()
        libraryPagination.reset()
        likesPagination.reset()
        bookmarksPagination.reset()
        collectionsPagination.reset()

        _isLoadingMoreActivity.value = false
        _hasMoreActivity.value = true
        _isLoadingMoreLibrary.value = false
        _hasMoreLibrary.value = true
        _isLoadingMoreLikes.value = false
        _hasMoreLikes.value = true
        _isLoadingMoreBookmarks.value = false
        _hasMoreBookmarks.value = false
        _isLoadingMoreCollections.value = false
        _hasMoreCollections.value = false

        _activityContent.value = emptyList()
        _libraryContent.value = emptyList()
        _likedContent.value = emptyList()
        _collectionContent.value = emptyList()

        _section1State.value = ContentState.Loading
        _section2State.value = ContentState.Loading
    }
}