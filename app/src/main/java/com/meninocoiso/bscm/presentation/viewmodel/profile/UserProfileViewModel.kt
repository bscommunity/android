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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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
 *
 * ## Reactivity strategy
 *
 * Likes and bookmarks use a two-layer approach:
 *
 *  1. **Paginated fetch** (`fetchPaged`) — handles initial load, "load more", and
 *     pull-to-refresh from the API
 *
 *  2. **Room observer** — a `Flow<List<Chart>>` from `ChartDao` that Room re-emits
 *     automatically whenever any coroutine writes `liked_at` or `bookmarked_at`,
 *     including writes made by [InteractionViewModel] from the details screen.
 *
 * The observer skips the very first emission (`drop(1)`) because `fetchPaged`
 * already populates the list on launch; we only want to react to *changes*.
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
    // Observer jobs — kept so we can cancel/restart on resetAll()
    // -------------------------------------------------------------------------

    private var likesObserverJob: Job? = null
    private var bookmarksObserverJob: Job? = null

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
     *
     * The Room observers are started here on first visit so they're only active
     * while the relevant tab has been opened at least once.
     */
    fun onTabSelected(index: Int) {
        when (index) {
            0 -> if (_uiState.value.likes.isEmpty) {
                fetchUserLikes()
                startLikesObserver()
            }
            1 -> if (_uiState.value.collections.bookmarks.items.isEmpty()) {
                fetchUserCollections()
                startBookmarksObserver()
                startCollectionsObserver()
            }
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
    // Room observers — react to writes from InteractionViewModel
    // -------------------------------------------------------------------------

    /**
     * Starts observing liked charts from Room.
     *
     * Instead of blindly dropping the first emission with `drop(1)`, we check
     * whether the initial paginated fetch is still in flight. This handles
     * the deep-link flow where Room already has fresh data when the observer
     * starts — in that case, the first emission should be applied, not skipped.
     *
     * On subsequent emissions (i.e. a real change happened), we surgically
     * update only [PagedSection.items] while preserving all other pagination
     * metadata (hasMore, isLoadingMore, state, etc.) so infinite scroll still works.
     */
    private fun startLikesObserver() {
        if (likesObserverJob?.isActive == true) return // already watching

        likesObserverJob = viewModelScope.launch {
            meRepository.observeLikes()
                .catch { e -> Log.e(TAG, "Likes observer error", e) }
                .collect { freshLikes ->
                    // Only apply if we already have a stable first page,
                    // otherwise we race with fetchPaged and may flash empty→full→correct
                    val current = _uiState.value.likes
                    if (current.state !is ContentState.Loading || current.items.isNotEmpty()) {
                        Log.d(TAG, "Likes observer fired: ${freshLikes.size} items")
                        _uiState.update { state ->
                            state.copy(
                                likes = state.likes.copy(items = freshLikes)
                            )
                        }
                    }
                }
        }
    }

    /**
     * Starts observing bookmarked charts from Room.
     *
     * Instead of blindly dropping the first emission with `drop(1)`, we check
     * whether the initial paginated fetch is still in flight. This handles
     * the deep-link flow where Room already has fresh data when the observer
     * starts — in that case, the first emission should be applied, not skipped.
     *
     * Only the bookmark items inside [CollectionSectionState.bookmarks] are
     * updated; the custom-collections section is left untouched.
     */
    private fun startBookmarksObserver() {
        if (bookmarksObserverJob?.isActive == true) return

        bookmarksObserverJob = viewModelScope.launch {
            meRepository.observeBookmarks()
                .catch { e -> Log.e(TAG, "Bookmarks observer error", e) }
                .collect { freshBookmarks ->
                    // Only apply if we already have a stable first page,
                    // otherwise we race with fetchPaged and may flash empty→full→correct
                    val current = _uiState.value.collections.bookmarks
                    if (current.state !is ContentState.Loading || current.items.isNotEmpty()) {
                        Log.d(TAG, "Bookmarks observer fired: ${freshBookmarks.size} items")
                        _uiState.update { state ->
                            val updatedBookmarksSection = state.collections.bookmarks
                                .copy(items = freshBookmarks)
                            val bookmarksCollection = buildBookmarksCollection()
                            state.copy(
                                collections = state.collections.copy(
                                    bookmarks = updatedBookmarksSection,
                                    items = mergeCollections(
                                        bookmarksCollection = bookmarksCollection,
                                        customCollections = null,
                                    ),
                                )
                            )
                        }
                    }
                }
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
                val bookmarksCollection = buildBookmarksCollection()
                state.copy(
                    collections = state.collections.copy(
                        items = mergeCollections(bookmarksCollection, customCollections = null),
                        state = section.state,
                        bookmarks = section,
                    )
                )
            }
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
                state.copy(
                    collections = state.collections.copy(
                        items = mergeCollections(customCollections = section.items),
                        state = section.state,
                        customCollections = section,
                    )
                )
            }
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
    private fun buildBookmarksCollection(): Collection {
        val existing = _uiState.value.collections.items
            .firstOrNull { it.kind == CollectionKind.BOOKMARKS }

        return existing?.copy()
            ?: Collection(
                id        = "bookmarks",
                userId    = (_profile.value as? ContentResult.Success)?.data?.user?.id ?: "unknown",
                kind      = CollectionKind.BOOKMARKS,
                name      = "Bookmarks",
                isPublic  = false,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )
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

    private var collectionsObserverJob: Job? = null

    private fun startCollectionsObserver() {
        if (collectionsObserverJob?.isActive == true) return

        collectionsObserverJob = viewModelScope.launch {
            collectionRepository.observeUserCollections()
                .catch { e -> Log.e(TAG, "Collections observer error", e) }
                .collect { freshCollections ->
                    // Only apply if we already have a stable first page
                    val current = _uiState.value.collections.customCollections
                    if (current.state !is ContentState.Loading || current.items.isNotEmpty()) {
                        _uiState.update { state ->
                            state.copy(
                                collections = state.collections.copy(
                                    customCollections = state.collections.customCollections
                                        .copy(items = freshCollections),
                                    items = mergeCollections(customCollections = freshCollections),
                                )
                            )
                        }
                    }
                }
        }
    }

    // -------------------------------------------------------------------------
    // Reset
    // -------------------------------------------------------------------------

    private fun resetAll() {
        // Cancel observers — they'll be restarted when tabs are visited again
        likesObserverJob?.cancel()
        likesObserverJob = null
        bookmarksObserverJob?.cancel()
        bookmarksObserverJob = null
        collectionsObserverJob?.cancel()
        collectionsObserverJob = null

        likesPagination.reset()
        bookmarksPagination.reset()
        collectionsPagination.reset()
        _profile.value = ContentResult.Loading
        _uiState.value = UserProfileUiState()
    }
}