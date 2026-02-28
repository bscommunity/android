package com.meninocoiso.bscm.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.data.remote.ApiException
import com.meninocoiso.bscm.data.remote.dto.activity.ActivityItemResponse
import com.meninocoiso.bscm.data.remote.dto.user.UserProfileResponse
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Collection
import com.meninocoiso.bscm.domain.repository.CollectionRepository
import com.meninocoiso.bscm.domain.repository.ProfileRepository
import com.meninocoiso.bscm.domain.result.ContentResult
import com.meninocoiso.bscm.presentation.viewmodel.profile.BaseProfileViewModel
import com.meninocoiso.bscm.presentation.viewmodel.profile.PagedSection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "PublicProfileViewModel"

/**
 * ViewModel for viewing another user's public profile.
 *
 * Because pagination logic now lives in [BaseProfileViewModel], this class is
 * purely about *what* to fetch and *where* to store results — not *how* to paginate.
 */
@HiltViewModel
class PublicProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val collectionRepository: CollectionRepository,
) : BaseProfileViewModel() {

    // -------------------------------------------------------------------------
    // Profile header
    // -------------------------------------------------------------------------

    private val _profile =
        MutableStateFlow<ContentResult<UserProfileResponse>>(ContentResult.Loading)
    val profile: StateFlow<ContentResult<UserProfileResponse>> = _profile.asStateFlow()

    // -------------------------------------------------------------------------
    // UI state
    // -------------------------------------------------------------------------

    data class PublicProfileUiState(
        val activity: PagedSection<ActivityItemResponse> = PagedSection(),
        val library: PagedSection<CatalogItem> = PagedSection(),
        val customCollections: PagedSection<Collection> = PagedSection(),
        val isFollowing: Boolean = false,
        val isFollowLoading: Boolean = false,
    )

    private val _uiState = MutableStateFlow(PublicProfileUiState())
    val uiState: StateFlow<PublicProfileUiState> = _uiState.asStateFlow()

    // -------------------------------------------------------------------------
    // Pagination cursors
    // -------------------------------------------------------------------------

    private val activityPagination = PaginationState(pageSize = 20)
    private val libraryPagination = PaginationState(pageSize = 20)
    private val collectionsPagination = PaginationState(pageSize = 20)

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    fun loadProfile(username: String) {
        // Skip reload if the profile is already successfully loaded for this username.
        val current = _profile.value
        if (current is ContentResult.Success && current.data.user.username == username) {
            Log.d(TAG, "Profile already loaded for: $username, skipping reload")
            return
        }
        resetAll()
        viewModelScope.launch {
            Log.d(TAG, "Loading profile for: $username")
            profileRepository.getProfileHeader(username)
                .onSuccess { profile ->
                    _profile.value = ContentResult.Success(profile)
                    _uiState.update { it.copy(isFollowing = profile.isFollowing == true) }
                    Log.d(TAG, "Profile loaded: $username")
                }
                .onFailure { error ->
                    _profile.value = ContentResult.Error(error.message ?: "Failed to load profile")
                    Log.e(TAG, "Error loading profile: $username", error)
                }
        }
    }

    /**
     * Called when the user taps a tab. Fetches lazily on first visit.
     *
     * @param userId The resolved user ID (available after [loadProfile] succeeds).
     */
    fun onTabSelected(userId: String, index: Int) {
        when (index) {
            0 -> if (_uiState.value.activity.isEmpty) fetchActivity(userId)
            1 -> {
                if (_uiState.value.library.isEmpty) fetchLibrary(userId)
                if (_uiState.value.customCollections.isEmpty) fetchCollections(userId)
            }
        }
    }

    /**
     * Follow toggle — waits for the server response before updating local state.
     * Shows a loading indicator while the request is in flight.
     */
    fun toggleFollow(userId: String, username: String) = viewModelScope.launch {
        val target = !_uiState.value.isFollowing
        _uiState.update { it.copy(isFollowLoading = true) }
        Log.d(TAG, "Toggling follow → $target for $userId")

        val result = if (target) profileRepository.followUser(userId, username)
        else profileRepository.unfollowUser(userId, username)

        result
            .onSuccess {
                _uiState.update { it.copy(isFollowing = target, isFollowLoading = false) }
                Log.d(TAG, "Follow toggled → $target for $userId")
            }
            .onFailure { error ->
                if (error is ApiException && error.status.value == 400) {
                    Log.w(
                        TAG,
                        "Received 400 Bad Request when trying to ${if (target) "follow" else "unfollow"} user $userId. " +
                                "This may be due to an optimistic UI update that is now out of sync with the server state. Reverting to previous state.",
                        error
                    )
                    // Handle 400 Bad Request (e.g. trying to follow an already-followed user) as a non-error by reverting the optimistic UI change.
                    _uiState.update { it.copy(isFollowing = target, isFollowLoading = false) }
                } else {
                    _uiState.update { it.copy(isFollowLoading = false) }
                    emitSnackbar("Failed to ${if (target) "follow" else "unfollow"} user")
                    Log.e(TAG, "Toggle follow failed for $userId", error)
                }
            }
    }

    fun fetchActivity(userId: String) = fetchPaged(
        pagination = activityPagination,
        fetch = { limit, offset, _ ->
            profileRepository.getActivity(userId = userId, limit = limit, offset = offset)
        },
        getItems = { _uiState.value.activity.items },
        setSection = { section -> _uiState.update { it.copy(activity = section) } },
    )

    fun refreshActivity(userId: String) = refreshPaged(
        pagination = activityPagination,
        useCache = false,
        fetch = { limit, offset, _ ->
            profileRepository.getActivity(
                userId = userId,
                limit = limit,
                offset = offset,
                useCache = false
            )
        },
        getItems = { _uiState.value.activity.items },
        getSection = { _uiState.value.activity },
        setSection = { section -> _uiState.update { it.copy(activity = section) } },
        onFailureWithData = { emitSnackbar("Failed to update activity feed") },
    )

    fun fetchLibrary(userId: String) = fetchPaged(
        pagination = libraryPagination,
        fetch = { limit, offset, _ ->
            profileRepository.getUserCharts(userId = userId, limit = limit, offset = offset)
        },
        getItems = { _uiState.value.library.items },
        setSection = { section ->
            _uiState.update { it.copy(library = section) }
            // Log.d(TAG, "Library updated: ${section.items.size} items for $userId")
        },
    )

    fun refreshLibrary(userId: String) = refreshPaged(
        pagination = libraryPagination,
        useCache = false,
        fetch = { limit, offset, _ ->
            profileRepository.getUserCharts(
                userId = userId,
                limit = limit,
                offset = offset,
                useCache = false
            )
        },
        getItems = { _uiState.value.library.items },
        getSection = { _uiState.value.library },
        setSection = { section -> _uiState.update { it.copy(library = section) } },
        onFailureWithData = { emitSnackbar("Failed to update library") },
    )

    fun fetchCollections(userId: String) = fetchPaged(
        pagination = collectionsPagination,
        fetch = { limit, offset, _ ->
            collectionRepository.getUserCollections(userId = userId, limit = limit, offset = offset)
        },
        getItems = { _uiState.value.customCollections.items },
        setSection = { section ->
            _uiState.update { it.copy(customCollections = section) }
            // Log.d(TAG, "Collections updated: ${section.items.size} items for $userId")
        },
    )

    fun refreshCollections(userId: String) = refreshPaged(
        pagination = collectionsPagination,
        useCache = false,
        fetch = { limit, offset, _ ->
            collectionRepository.getUserCollections(
                userId = userId,
                limit = limit,
                offset = offset,
                useCache = false
            )
        },
        getItems = { _uiState.value.customCollections.items },
        getSection = { _uiState.value.customCollections },
        setSection = { section -> _uiState.update { it.copy(customCollections = section) } },
        onFailureWithData = { emitSnackbar("Failed to update collections") },
    )

    fun loadMoreActivity(userId: String) {
        if (_uiState.value.activity.isIdle) fetchActivity(userId)
    }

    fun loadMoreLibrary(userId: String) {
        if (_uiState.value.library.isIdle) fetchLibrary(userId)
    }

    fun loadMoreCollections(userId: String) {
        if (_uiState.value.customCollections.isIdle) fetchCollections(userId)
    }

    // -------------------------------------------------------------------------
    // Reset
    // -------------------------------------------------------------------------

    private fun resetAll() {
        activityPagination.reset()
        libraryPagination.reset()
        collectionsPagination.reset()
        _profile.value = ContentResult.Loading
        _uiState.value = PublicProfileUiState()
    }
}