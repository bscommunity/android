package com.meninocoiso.bscm.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.data.remote.dto.user.UserProfileResponse
import com.meninocoiso.bscm.domain.enums.CollectionKind
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Collection
import com.meninocoiso.bscm.domain.repository.CollectionRepository
import com.meninocoiso.bscm.domain.repository.MeRepository
import com.meninocoiso.bscm.domain.result.ContentResult
import com.meninocoiso.bscm.domain.result.ContentState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

private const val TAG = "UserProfileViewModel"

/**
 * ViewModel for the authenticated user's own profile.
 * Handles likes, bookmarks, and collections for the current user.
 */
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val meRepository: MeRepository,
    private val collectionRepository: CollectionRepository,
) : ViewModel() {
    private val _profile = MutableStateFlow<ContentResult<UserProfileResponse>>(ContentResult.Loading)
    val profile: StateFlow<ContentResult<UserProfileResponse>> = _profile.asStateFlow()

    private val likesPagination = PaginationState(pageSize = 20)
    private val bookmarksPagination = PaginationState(pageSize = 20)
    private val collectionsPagination = PaginationState(pageSize = 20)

    data class PaginationMeta(
        val isLoadingMore: Boolean = false,
        val hasMore: Boolean = true
    )

    data class PagedSectionState<T>(
        val items: List<T> = emptyList(),
        val state: ContentState = ContentState.Loading,
        val pagination: PaginationMeta = PaginationMeta()
    )

    data class CollectionSectionState(
        val items: List<Collection> = emptyList(),
        val state: ContentState = ContentState.Loading,
        val bookmarks: PaginationMeta = PaginationMeta(),
        val customCollections: PaginationMeta = PaginationMeta()
    )

    data class UserProfileUiState(
        val likes: PagedSectionState<CatalogItem> = PagedSectionState(),
        val collections: CollectionSectionState = CollectionSectionState()
    )

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    /**
     * Loads the profile for the authenticated user.
     */
    fun loadProfile() {
        resetAll()
        _profile.value = ContentResult.Loading

        viewModelScope.launch {
            Log.d(TAG, "Fetching owner profile")
            meRepository.getProfile().onSuccess { profile ->
                _profile.value = ContentResult.Success(profile)
                Log.d(TAG, "Owner profile loaded successfully")
            }.onFailure {
                _profile.value = ContentResult.Error(it.message ?: "Failed to load profile")
                Log.e(TAG, "Error loading owner profile", it)
            }
        }
    }

    /**
     * Handles tab selection for the user profile view.
     * Fetches the appropriate content for the selected tab if not already loaded.
     *
     * @param index The index of the selected tab (0 for likes, 1 for collections).
     */
    fun onTabSelected(index: Int) {
        when (index) {
            0 -> if (_uiState.value.likes.items.isEmpty()) fetchUserLikes(reset = false)
            1 -> if (_uiState.value.collections.items.isEmpty()) fetchUserCollections(reset = false)
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
                _uiState.update { current ->
                    current.copy(
                        likes = current.likes.copy(
                            items = emptyList(),
                            state = ContentState.Loading,
                            pagination = PaginationMeta()
                        )
                    )
                }
            }

            if (likesPagination.isLoadingMore || !likesPagination.hasMore) return@launch

            likesPagination.isLoadingMore = true
            _uiState.update { current ->
                current.copy(
                    likes = current.likes.copy(
                        pagination = current.likes.pagination.copy(isLoadingMore = true)
                    )
                )
            }

            Log.d(
                TAG,
                "Fetching likes, limit: ${likesPagination.pageSize}, offset: ${likesPagination.nextOffset()}"
            )
            val result = meRepository.getLikes(
                limit = likesPagination.pageSize,
                offset = likesPagination.nextOffset(),
                useCache = !reset
            )

            result.onSuccess { data ->
                val updated = if (likesPagination.currentPage == 0) {
                    data
                } else {
                    _uiState.value.likes.items + data
                }
                likesPagination.hasMore = data.size >= likesPagination.pageSize
                likesPagination.advancePage()
                _uiState.update { current ->
                    current.copy(
                        likes = current.likes.copy(
                            items = updated,
                            state = ContentState.Success,
                            pagination = current.likes.pagination.copy(
                                hasMore = likesPagination.hasMore
                            )
                        )
                    )
                }

                Log.d(TAG, "Fetched likes: ${data.size}, total: ${updated.size}")
            }.onFailure {
                likesPagination.hasMore = false
                _uiState.update { current ->
                    current.copy(
                        likes = current.likes.copy(
                            state = ContentState.Error,
                            pagination = current.likes.pagination.copy(hasMore = false)
                        )
                    )
                }
                Log.e(TAG, "Error fetching likes", it)
            }

            likesPagination.isLoadingMore = false
            _uiState.update { current ->
                current.copy(
                    likes = current.likes.copy(
                        pagination = current.likes.pagination.copy(isLoadingMore = false)
                    )
                )
            }
        }
    }

    /**
     * Fetches the user's collections (bookmarks and custom collections)
     *
     * @param reset Whether to reset the pagination and content.
     */
    fun fetchUserCollections(reset: Boolean = false) {
        viewModelScope.launch {
            if (reset) {
                bookmarksPagination.reset()
                collectionsPagination.reset()
                _uiState.update { current ->
                    current.copy(
                        collections = current.collections.copy(
                            items = emptyList(),
                            state = ContentState.Loading,
                            bookmarks = PaginationMeta(),
                            customCollections = PaginationMeta()
                        )
                    )
                }
            }

            Log.d(TAG, "Fetching user collections, reset: $reset")
            fetchBookmarksInternal(!reset)
            fetchCustomCollectionsInternal(!reset)
        }
    }

    /**
     * Loads more likes for the current user.
     */
    fun loadMoreLikes() {
        if (_uiState.value.likes.state == ContentState.Loading) return
        Log.d(TAG, "Loading more likes")
        fetchUserLikes(reset = false)
    }

    /**
     * Loads more bookmarks for the current user.
     */
    fun loadMoreBookmarks() {
        if (_uiState.value.collections.state == ContentState.Loading) return
        Log.d(TAG, "Loading more bookmarks")
        viewModelScope.launch { fetchBookmarksInternal() }
    }

    /**
     * Loads more collections for the current user.
     */
    fun loadMoreCollections() {
        if (_uiState.value.collections.state == ContentState.Loading) return
        Log.d(TAG, "Loading more custom collections")
        viewModelScope.launch { fetchCustomCollectionsInternal() }
    }

    /**
     * Fetches bookmarks internally with pagination support.
     */
    private suspend fun fetchBookmarksInternal(useCache: Boolean = true) {
        if (bookmarksPagination.isLoadingMore || !bookmarksPagination.hasMore) return

        bookmarksPagination.isLoadingMore = true
        _uiState.update { current ->
            current.copy(
                collections = current.collections.copy(
                    bookmarks = current.collections.bookmarks.copy(isLoadingMore = true)
                )
            )
        }
        Log.d(
            TAG,
            "Fetching bookmarks, limit: ${bookmarksPagination.pageSize}, offset: ${bookmarksPagination.nextOffset()}"
        )
        val result = meRepository.getBookmarks(
            limit = bookmarksPagination.pageSize,
            offset = bookmarksPagination.nextOffset(),
            useCache = useCache
        )

        result.onSuccess { data ->
            val existingBookmarksCollection =
                _uiState.value.collections.items.firstOrNull { it.kind == CollectionKind.BOOKMARKS }
            val existing = existingBookmarksCollection?.items ?: emptyList()
            val updatedBookmarks = if (bookmarksPagination.currentPage == 0) data else {
                existing + data
            }

            bookmarksPagination.hasMore = data.size >= bookmarksPagination.pageSize
            bookmarksPagination.advancePage()
            updateCollectionContent(
                updatedBookmarks = updatedBookmarks,
                collectionState = ContentState.Success,
                bookmarksMeta = PaginationMeta(
                    isLoadingMore = false,
                    hasMore = bookmarksPagination.hasMore
                )
            )
            Log.d(TAG, "Fetched bookmarks: ${data.size}, total: ${updatedBookmarks.size}")
        }.onFailure {
            bookmarksPagination.hasMore = false
            _uiState.update { current ->
                current.copy(
                    collections = current.collections.copy(
                        state = ContentState.Error,
                        bookmarks = current.collections.bookmarks.copy(hasMore = false)
                    )
                )
            }
            Log.e(TAG, "Error fetching bookmarks", it)
        }

        bookmarksPagination.isLoadingMore = false
        _uiState.update { current ->
            current.copy(
                collections = current.collections.copy(
                    bookmarks = current.collections.bookmarks.copy(isLoadingMore = false)
                )
            )
        }
    }

    /**
     * Fetches custom collections internally with pagination support.
     */
    private suspend fun fetchCustomCollectionsInternal(useCache: Boolean = true) {
        if (collectionsPagination.isLoadingMore || !collectionsPagination.hasMore) return

        collectionsPagination.isLoadingMore = true
        _uiState.update { current ->
            current.copy(
                collections = current.collections.copy(
                    customCollections = current.collections.customCollections.copy(isLoadingMore = true)
                )
            )
        }
        Log.d(
            TAG,
            "Fetching custom collections, limit: ${collectionsPagination.pageSize}, offset: ${collectionsPagination.nextOffset()}"
        )
        val result = collectionRepository.getUserCollections(
            limit = collectionsPagination.pageSize,
            offset = collectionsPagination.nextOffset(),
            useCache = useCache
        )

        result.onSuccess { data ->
            val existingCustom = _uiState.value.collections.items.filter { it.kind == CollectionKind.USER }
            val updatedCustom =
                if (collectionsPagination.currentPage == 0) data else existingCustom + data
            collectionsPagination.hasMore = data.size >= collectionsPagination.pageSize
            collectionsPagination.advancePage()
            updateCollectionContent(
                customCollections = updatedCustom,
                collectionState = ContentState.Success,
                customMeta = PaginationMeta(
                    isLoadingMore = false,
                    hasMore = collectionsPagination.hasMore
                )
            )
            Log.d(TAG, "Fetched custom collections: ${data.size}, total: ${updatedCustom.size}")
        }.onFailure {
            collectionsPagination.hasMore = false
            _uiState.update { current ->
                current.copy(
                    collections = current.collections.copy(
                        state = ContentState.Error,
                        customCollections = current.collections.customCollections.copy(hasMore = false)
                    )
                )
            }
            Log.e(TAG, "Error fetching custom collections", it)
        }

        collectionsPagination.isLoadingMore = false
        _uiState.update { current ->
            current.copy(
                collections = current.collections.copy(
                    customCollections = current.collections.customCollections.copy(isLoadingMore = false)
                )
            )
        }
    }

    /**
     * Updates the collection content by merging bookmarks and custom collections.
     *
     * @param updatedBookmarks The updated list of bookmarks.
     * @param customCollections The updated list of custom collections.
     */
    private fun updateCollectionContent(
        updatedBookmarks: List<CatalogItem>? = null,
        customCollections: List<Collection>? = null,
        collectionState: ContentState? = null,
        bookmarksMeta: PaginationMeta? = null,
        customMeta: PaginationMeta? = null
    ) {
        val items = _uiState.value.collections.items
        val existingBookmarks = items.find { it.kind == CollectionKind.BOOKMARKS }
        val existingCustom = items.filter { it.kind == CollectionKind.USER }

        if (updatedBookmarks == null && customCollections == null && collectionState == null && bookmarksMeta == null && customMeta == null) {
            return
        }

        val bookmarksCollection: Collection? = when {
            updatedBookmarks != null && existingBookmarks != null -> existingBookmarks.copy(items = updatedBookmarks)
            updatedBookmarks != null -> Collection(
                id = "bookmarks",
                userId = _profile.value.let { if (it is ContentResult.Success) it.data.user.id else "user" },
                kind = CollectionKind.BOOKMARKS,
                name = "Bookmarks",
                isPublic = false,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                items = updatedBookmarks
            )

            else -> existingBookmarks
        }

        val customs = customCollections ?: existingCustom

        val merged = mutableListOf<Collection>()
        bookmarksCollection?.let { merged.add(it) }
        merged.addAll(customs)

        _uiState.update { current ->
            current.copy(
                collections = current.collections.copy(
                    items = merged,
                    state = collectionState ?: current.collections.state,
                    bookmarks = bookmarksMeta ?: current.collections.bookmarks,
                    customCollections = customMeta ?: current.collections.customCollections
                )
            )
        }
    }

    /**
     * Resets all pagination states and content to their initial values.
     */
    private fun resetAll() {
        likesPagination.reset()
        bookmarksPagination.reset()
        collectionsPagination.reset()

        _uiState.value = UserProfileUiState()
    }
}
