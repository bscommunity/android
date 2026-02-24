package com.meninocoiso.bscm.presentation.viewmodel.profile

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.data.remote.dto.user.UserProfileResponse
import com.meninocoiso.bscm.domain.enums.CollectionKind
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Collection
import com.meninocoiso.bscm.domain.repository.CollectionRepository
import com.meninocoiso.bscm.domain.repository.MeRepository
import com.meninocoiso.bscm.domain.result.ContentResult
import com.meninocoiso.bscm.domain.result.ContentState
import com.meninocoiso.bscm.presentation.viewmodel.PaginationState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
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
 *
 * Extends [BaseProfileViewModel] to inherit the generic [fetchPaged] / [refreshPaged]
 * helpers, so pagination boilerplate is written exactly once.
 */
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val meRepository: MeRepository,
    private val collectionRepository: CollectionRepository,
) : BaseProfileViewModel() {

    // -------------------------------------------------------------------------
    // Profile header
    // -------------------------------------------------------------------------

    private val _profile = MutableStateFlow<ContentResult<UserProfileResponse>>(ContentResult.Loading)
    val profile: StateFlow<ContentResult<UserProfileResponse>> = _profile.asStateFlow()

    // -------------------------------------------------------------------------
    // UI state
    // -------------------------------------------------------------------------

    /**
     * The Collections tab needs two independent pagination cursors (bookmarks + custom),
     * but they share a single merged [items] list rendered in the UI.
     */
    data class CollectionSectionState(
        val items: List<Collection> = emptyList(),
        val state: ContentState = ContentState.Loading,
        val isRefreshing: Boolean = false,
        val bookmarks: PagedSection<CatalogItem> = PagedSection(),
        val customCollections: PagedSection<Collection> = PagedSection(),
    )

    data class UserProfileUiState(
        val likes: PagedSection<CatalogItem> = PagedSection(),
        val collections: CollectionSectionState = CollectionSectionState(),
    )

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    // -------------------------------------------------------------------------
    // Pagination cursors (mutable bookmarks — not part of UI state)
    // -------------------------------------------------------------------------

    private val likesPagination        = PaginationState(pageSize = 20)
    private val bookmarksPagination    = PaginationState(pageSize = 20)
    private val collectionsPagination  = PaginationState(pageSize = 20)

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    fun loadProfile() {
        resetAll()
        viewModelScope.launch {
            meRepository.getProfile()
                .onSuccess { _profile.value = ContentResult.Success(it) }
                .onFailure { _profile.value = ContentResult.Error(it.message ?: "Failed to load profile") }
        }
    }

    /**
     * Called when the user taps a tab. Loads data lazily — only fetches if the
     * tab's list is still empty (i.e. first visit).
     */
    fun onTabSelected(index: Int) {
        when (index) {
            0 -> if (_uiState.value.likes.isEmpty)               fetchUserLikes()
            1 -> if (_uiState.value.collections.items.isEmpty()) fetchUserCollections()
        }
    }

    fun fetchUserLikes() = fetchPaged(
        pagination = likesPagination,
        fetch = { limit, offset, cache -> meRepository.getLikes(limit, offset, cache) },
        getItems = { _uiState.value.likes.items },
        setSection = { section -> _uiState.update { it.copy(likes = section) } },
    )

    fun refreshUserLikes() = refreshPaged(
        pagination = likesPagination,
        fetch = { limit, offset, cache -> meRepository.getLikes(limit, offset, cache) },
        getItems = { _uiState.value.likes.items },
        getSection = { _uiState.value.likes },
        setSection = { section -> _uiState.update { it.copy(likes = section) } },
        onFailureWithData = { emitSnackbar("Falha ao atualizar curtidas") },
    )

    fun loadMoreLikes() {
        if (_uiState.value.likes.isIdle) fetchUserLikes()
    }

    /**
     * Fetches both sub-sections of the Collections tab in parallel using [async].
     * This cuts wall-clock time roughly in half compared to sequential fetches.
     */
    fun fetchUserCollections() = viewModelScope.launch {
        val bookmarksJob    = fetchBookmarks()
        val collectionsJob  = fetchCustomCollections()
        bookmarksJob.join()
        collectionsJob.join()
    }

    fun refreshUserCollections() = viewModelScope.launch {
        _uiState.update { it.copy(collections = it.collections.copy(isRefreshing = true)) }
        var anyFailed = false

        try {
            val b = fetchBookmarks(reset = true, useCache = false,
                onFailureWithData = { anyFailed = true })
            val c = fetchCustomCollections(reset = true, useCache = false,
                onFailureWithData = { anyFailed = true })
            b.join(); c.join()
            if (anyFailed) emitSnackbar("Failed to refresh collections")
        } finally {
            _uiState.update { it.copy(collections = it.collections.copy(isRefreshing = false)) }
        }
    }

    fun loadMoreBookmarks() {
        if (_uiState.value.collections.bookmarks.isIdle) {
            viewModelScope.launch { fetchBookmarks().join() }
        }
    }

    fun loadMoreCollections() {
        if (_uiState.value.collections.customCollections.isIdle) {
            viewModelScope.launch { fetchCustomCollections().join() }
        }
    }

    // -------------------------------------------------------------------------
    // Internal fetch helpers
    // -------------------------------------------------------------------------

    private fun fetchBookmarks(
        reset: Boolean = false,
        useCache: Boolean = true,
        onFailureWithData: (suspend () -> Unit)? = null,
    ) = fetchPaged(
        pagination = bookmarksPagination,
        reset = reset,
        useCache = useCache,
        showSkeletonWhen = { _uiState.value.collections.bookmarks.items.isEmpty() },
        fetch = { limit, offset, cache -> meRepository.getBookmarks(limit, offset, cache) },
        getItems = {
            _uiState.value.collections.bookmarks.items
        },
        setSection = { section ->
            _uiState.update { state ->
                val bookmarksCollection = buildBookmarksCollection(section.items)
                state.copy(
                    collections = state.collections.copy(
                        items = mergeCollections(bookmarksCollection, customCollections = null),
                        state = section.state,
                        bookmarks = section,
                    )
                )
            }
            Log.d(TAG, "Bookmarks updated: ${section.items.size} items")
        },
        onFailureWithData = onFailureWithData,
    )

    private fun fetchCustomCollections(
        reset: Boolean = false,
        useCache: Boolean = true,
        onFailureWithData: (suspend () -> Unit)? = null,
    ) = fetchPaged(
        pagination = collectionsPagination,
        reset = reset,
        useCache = useCache,
        showSkeletonWhen = { _uiState.value.collections.customCollections.items.isEmpty() },
        fetch = { limit, offset, cache ->
            collectionRepository.getUserCollections(limit = limit, offset = offset, useCache = cache)
        },
        getItems = {
            _uiState.value.collections.customCollections.items
        },
        setSection = { section ->
            _uiState.update { state ->
                Log.d(TAG, "Merging ${section.items} custom collections with existing bookmarks")
                state.copy(
                    collections = state.collections.copy(
                        items = mergeCollections(customCollections = section.items),
                        state = section.state,
                        customCollections = section,
                    )
                )
            }
            Log.d(TAG, "Custom collections updated: ${section.items.size} items")
        },
        onFailureWithData = onFailureWithData,
    )

    // -------------------------------------------------------------------------
    // Collection merge helpers
    // -------------------------------------------------------------------------

    /**
     * Converts a raw [List<CatalogItem>] (bookmark items) into a [Collection] wrapper
     * so it can live alongside custom collections in one unified list.
     *
     * If a Bookmarks collection already exists in state it is reused (preserving its
     * server-side id / metadata); otherwise a local placeholder is created.
     */
    private fun buildBookmarksCollection(items: List<CatalogItem>): Collection {
        val existing = _uiState.value.collections.items
            .firstOrNull { it.kind == CollectionKind.BOOKMARKS }

        return existing?.copy()?.also { it.items = items }
            ?: Collection(
                id        = "bookmarks",
                userId    = (_profile.value as? ContentResult.Success)?.data?.user?.id ?: "unknown",
                kind      = CollectionKind.BOOKMARKS,
                name      = "Bookmarks",
                isPublic  = false,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            ).also { it.items = items }
    }

    /**
     * Produces the merged [List<Collection>] shown in the UI.
     * Bookmarks always appear first; custom collections follow.
     *
     * Passing null for either argument keeps the currently stored value for that slot.
     */
    private fun mergeCollections(
        bookmarksCollection: Collection? = null,
        customCollections: List<Collection>? = null,
    ): List<Collection> {
        val existing = _uiState.value.collections.items
        val bookmarks = bookmarksCollection
            ?: existing.firstOrNull { it.kind == CollectionKind.BOOKMARKS }
        val custom = customCollections
            ?: existing.filter { it.kind == CollectionKind.USER }

        return buildList {
            bookmarks?.let { add(it) }
            addAll(custom)
        }
    }

    // -------------------------------------------------------------------------
    // Reset
    // -------------------------------------------------------------------------

    private fun resetAll() {
        likesPagination.reset()
        bookmarksPagination.reset()
        collectionsPagination.reset()
        _profile.value = ContentResult.Loading
        _uiState.value = UserProfileUiState()
    }
}