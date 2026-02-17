package com.meninocoiso.bscm.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.data.remote.dto.activity.ActivityItemResponse
import com.meninocoiso.bscm.data.remote.dto.user.UserProfileResponse
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.repository.ProfileRepository
import com.meninocoiso.bscm.domain.result.ContentResult
import com.meninocoiso.bscm.domain.result.ContentState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "PublicProfileViewModel"

/**
 * ViewModel for viewing public profiles (other users).
 * Handles activity and library content for public profiles.
 */
@HiltViewModel
class PublicProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
) : ViewModel() {
    private val _profile = MutableStateFlow<ContentResult<UserProfileResponse>>(ContentResult.Loading)
    val profile: StateFlow<ContentResult<UserProfileResponse>> = _profile.asStateFlow()

    private val activityPagination = PaginationState(pageSize = 20)
    private val libraryPagination = PaginationState(pageSize = 20)

    data class PagedSectionState<T>(
        val items: List<T> = emptyList(),
        val state: ContentState = ContentState.Loading,
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = true
    )

    data class PublicProfileUiState(
        val activity: PagedSectionState<ActivityItemResponse> = PagedSectionState(),
        val library: PagedSectionState<CatalogItem> = PagedSectionState(),
        val isFollowing: Boolean = false
    )

    private val _uiState = MutableStateFlow(PublicProfileUiState())
    val uiState: StateFlow<PublicProfileUiState> = _uiState.asStateFlow()

    /**
     * Loads the profile for the given username.
     *
     * @param username The username of the profile to load
     */
    fun loadProfile(username: String) {
        resetAll()
        _profile.value = ContentResult.Loading

        viewModelScope.launch {
            Log.d(TAG, "Fetching profile header for: $username")
            profileRepository.getProfileHeader(username).onSuccess { profile ->
                _profile.value = ContentResult.Success(profile)
                _uiState.update { current ->
                    current.copy(isFollowing = profile.isFollowing == true)
                }
                Log.d(TAG, "Profile header loaded successfully for: $username")
            }.onFailure {
                _profile.value = ContentResult.Error(it.message ?: "Failed to load profile")
                Log.e(TAG, "Error loading profile header for: $username", it)
            }
        }
    }

    /**
     * Handles tab selection for the public profile view.
     * Fetches the appropriate content for the selected tab if not already loaded.
     *
     * @param userId The ID of the user.
     * @param index The index of the selected tab (0 for activity, 1 for library).
     */
    fun onTabSelected(userId: String, index: Int) {
        when (index) {
            0 -> if (_uiState.value.activity.items.isEmpty()) fetchProfileActivity(userId, reset = true)
            1 -> if (_uiState.value.library.items.isEmpty()) fetchProfileLibrary(userId, reset = true)
        }
    }

    /**
     * Toggles the follow status for the given user.
     *
     * @param userId The ID of the user to follow or unfollow.
     */
    fun toggleFollow(userId: String) {
        viewModelScope.launch {
            val shouldFollow = !_uiState.value.isFollowing
            val result = if (shouldFollow) {
                Log.d(TAG, "Following user: $userId")
                profileRepository.followUser(userId)
            } else {
                Log.d(TAG, "Unfollowing user: $userId")
                profileRepository.unfollowUser(userId)
            }
            result.onSuccess {
                _uiState.update { current ->
                    current.copy(isFollowing = shouldFollow)
                }
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
                _uiState.update { current ->
                    current.copy(
                        activity = current.activity.copy(
                            items = emptyList(),
                            state = ContentState.Loading,
                            isLoadingMore = false,
                            hasMore = true
                        )
                    )
                }
            }

            if (activityPagination.isLoadingMore || !activityPagination.hasMore) return@launch

            activityPagination.isLoadingMore = true
            _uiState.update { current ->
                current.copy(activity = current.activity.copy(isLoadingMore = true))
            }
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
                val updated = if (activityPagination.currentPage == 0) {
                    entries
                } else {
                    _uiState.value.activity.items + entries
                }
                activityPagination.hasMore = entries.size >= activityPagination.pageSize
                activityPagination.advancePage()
                _uiState.update { current ->
                    current.copy(
                        activity = current.activity.copy(
                            items = updated,
                            state = ContentState.Success,
                            hasMore = activityPagination.hasMore
                        )
                    )
                }
                Log.d(
                    TAG,
                    "Fetched activity for userId: $userId, entries: ${entries.size}, total: ${updated.size}"
                )
            }.onFailure {
                activityPagination.hasMore = false
                _uiState.update { current ->
                    current.copy(
                        activity = current.activity.copy(
                            state = ContentState.Error,
                            hasMore = false
                        )
                    )
                }
                Log.e(TAG, "Error fetching activity for userId: $userId", it)
            }

            activityPagination.isLoadingMore = false
            _uiState.update { current ->
                current.copy(activity = current.activity.copy(isLoadingMore = false))
            }
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
                _uiState.update { current ->
                    current.copy(
                        library = current.library.copy(
                            items = emptyList(),
                            state = ContentState.Loading,
                            isLoadingMore = false,
                            hasMore = true
                        )
                    )
                }
            }

            if (libraryPagination.isLoadingMore || !libraryPagination.hasMore) return@launch

            libraryPagination.isLoadingMore = true
            _uiState.update { current ->
                current.copy(library = current.library.copy(isLoadingMore = true))
            }

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
                val updated = if (libraryPagination.currentPage == 0) {
                    data
                } else {
                    _uiState.value.library.items + data
                }
                libraryPagination.hasMore = data.size >= libraryPagination.pageSize
                libraryPagination.advancePage()
                _uiState.update { current ->
                    current.copy(
                        library = current.library.copy(
                            items = updated,
                            state = ContentState.Success,
                            hasMore = libraryPagination.hasMore
                        )
                    )
                }
                Log.d(
                    TAG,
                    "Fetched library for userId: $userId, charts: ${data.size}, total: ${updated.size}"
                )
            }.onFailure {
                libraryPagination.hasMore = false
                _uiState.update { current ->
                    current.copy(
                        library = current.library.copy(
                            state = ContentState.Error,
                            hasMore = false
                        )
                    )
                }
                Log.e(TAG, "Error fetching library for userId: $userId", it)
            }

            libraryPagination.isLoadingMore = false
            _uiState.update { current ->
                current.copy(library = current.library.copy(isLoadingMore = false))
            }
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
     * Resets all pagination states and content to their initial values.
     */
    private fun resetAll() {
        activityPagination.reset()
        libraryPagination.reset()

        _uiState.value = PublicProfileUiState()
    }
}

