package com.meninocoiso.bscm.presentation.viewmodel.profile

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.enums.CollectionKind
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Collection
import com.meninocoiso.bscm.domain.repository.CollectionRepository
import com.meninocoiso.bscm.domain.repository.MeRepository
import com.meninocoiso.bscm.domain.result.ContentState
import com.meninocoiso.bscm.domain.result.UiText
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
 *     pull-to-refresh from the API.
 *
 *  2. **Room observer** — a `Flow<List<Chart>>` from `ChartDao` that Room re-emits
 *     automatically whenever any coroutine writes `liked_at` or `bookmarked_at`,
 *     including writes made by [InteractionViewModel] from the details screen.
 *
 * The observer only applies once `fetchPaged` has settled to avoid racing with the
 * initial load and flashing empty → full → correct.
 */
@HiltViewModel
class UserProfileViewModel @Inject constructor(
    private val meRepository: MeRepository,
    private val collectionRepository: CollectionRepository,
) : BaseProfileViewModel() {

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
    ) {
        /** (charts, tourPasses, themes) from the server, falling back to the chart total. */
        val likesCounts: Triple<Int, Int, Int>
            get() = likes.counts ?: Triple(likes.total ?: 0, 0, 0)

        /** (charts, tourPasses, themes) from the server, falling back to the chart total. */
        val bookmarksCounts: Triple<Int, Int, Int>
            get() = collections.bookmarks.counts ?: Triple(collections.bookmarks.total ?: 0, 0, 0)

        /** Total number of custom collections. */
        val collectionsCount: Int
            get() = collections.customCollections.total ?: 0
    }

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState.asStateFlow()

    // -------------------------------------------------------------------------
    // Pagination cursors (mutable bookmarks — not part of UI state)
    // -------------------------------------------------------------------------

    private val likesPagination = PaginationState(pageSize = 20)
    private val bookmarksPagination = PaginationState(pageSize = 20)
    private val collectionsPagination = PaginationState(pageSize = 20)

    // -------------------------------------------------------------------------
    // Observer jobs — kept so we can cancel/restart on resetAll()
    // -------------------------------------------------------------------------

    private var likesObserverJob: Job? = null
    private var bookmarksObserverJob: Job? = null
    private var collectionsObserverJob: Job? = null

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Called when the user taps a tab. Loads data lazily — only fetches if the
     * tab's list is still empty (i.e. first visit).
     */
    fun onTabSelected(index: Int) {
        when (index) {
            0 -> if (_uiState.value.likes.isEmpty) {
                viewModelScope.launch {
                    fetchUserLikes().join()
                    startLikesObserver()
                }
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
        getSection = { _uiState.value.likes },
        setSection = { section -> _uiState.update { it.copy(likes = section) } },
    )

    fun refreshUserLikes() = refreshPaged(
        pagination = likesPagination,
        fetch = { limit, offset, cache -> meRepository.getLikes(limit, offset, cache) },
        getSection = { _uiState.value.likes },
        setSection = { section -> _uiState.update { it.copy(likes = section) } },
        onFailureWithData = { emitSnackbar(UiText.Res(R.string.failed_to_update_likes)) },
    )

    fun loadMoreLikes() {
        if (_uiState.value.likes.isIdle) fetchUserLikes()
    }

    /**
     * Fetches both sub-sections of the Collections tab in parallel.
     * Cuts wall-clock time roughly in half compared to sequential fetches.
     */
    fun fetchUserCollections() = viewModelScope.launch {
        fetchBookmarks().join()
        fetchCustomCollections().join()
    }

    fun refreshUserCollections() = viewModelScope.launch {
        _uiState.update { it.copy(collections = it.collections.copy(isRefreshing = true)) }
        var anyFailed = false

        try {
            val b = fetchBookmarks(
                reset = true, useCache = false,
                onFailureWithData = { anyFailed = true })
            val c = fetchCustomCollections(
                reset = true, useCache = false,
                onFailureWithData = { anyFailed = true })
            b.join(); c.join()
            if (anyFailed) emitSnackbar(UiText.Res(R.string.failed_to_refresh_collections))
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

    private fun startLikesObserver() {
        if (likesObserverJob?.isActive == true) return

        likesObserverJob = viewModelScope.launch {
            meRepository.observeLikes()
                .catch { e -> Log.e(TAG, "Likes observer error", e) }
                .collect { freshLikes ->
                    val current = _uiState.value.likes
                    if (current.state !is ContentState.Loading || current.items.isNotEmpty()) {
                        Log.d(TAG, "Likes observer fired: ${freshLikes.size} items")
                        val updatedTotal = reconcileObservedTotal(
                            previousItems = current.items,
                            freshItems = freshLikes,
                            previousTotal = current.total,
                            idSelector = { it.id },
                        )
                        _uiState.update { state ->
                            state.copy(
                                likes = state.likes.copy(
                                    items = freshLikes,
                                    total = updatedTotal,
                                )
                            )
                        }
                    }
                }
        }
    }

    private fun startBookmarksObserver() {
        if (bookmarksObserverJob?.isActive == true) return

        bookmarksObserverJob = viewModelScope.launch {
            meRepository.observeBookmarks()
                .catch { e -> Log.e(TAG, "Bookmarks observer error", e) }
                .collect { freshBookmarks ->
                    val current = _uiState.value.collections.bookmarks
                    if (current.state !is ContentState.Loading || current.items.isNotEmpty()) {
                        Log.d(TAG, "Bookmarks observer fired: ${freshBookmarks.size} items")
                        val updatedTotal = reconcileObservedTotal(
                            previousItems = current.items,
                            freshItems = freshBookmarks,
                            previousTotal = current.total,
                            idSelector = { it.id },
                        )
                        _uiState.update { state ->
                            val updatedBookmarks = state.collections.bookmarks
                                .copy(
                                    items = freshBookmarks,
                                    total = updatedTotal,
                                )
                            state.copy(
                                collections = state.collections.copy(
                                    bookmarks = updatedBookmarks,
                                    items = mergeCollections(
                                        bookmarksCollection = buildBookmarksCollection(),
                                        customCollections = null,
                                    ),
                                )
                            )
                        }
                    }
                }
        }
    }

    private fun startCollectionsObserver() {
        if (collectionsObserverJob?.isActive == true) return

        collectionsObserverJob = viewModelScope.launch {
            collectionRepository.observeUserCollections()
                .catch { e -> Log.e(TAG, "Collections observer error", e) }
                .collect { freshCollections ->
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
        getSection = { _uiState.value.collections.bookmarks },
        setSection = { section ->
            _uiState.update { state ->
                state.copy(
                    collections = state.collections.copy(
                        items = mergeCollections(buildBookmarksCollection(), customCollections = null),
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
    ) = fetchPaged<Collection>(
        pagination = collectionsPagination,
        reset = reset,
        useCache = useCache,
        showSkeletonWhen = { _uiState.value.collections.customCollections.items.isEmpty() },
        fetch = { limit, offset, cache ->
            val result = collectionRepository.getUserCollections(limit = limit, offset = offset, useCache = cache)
            result
        },
        getSection = { _uiState.value.collections.customCollections },
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
     * Converts the raw bookmark items into a [Collection] wrapper so it can live
     * alongside custom collections in one unified list.
     *
     * If a Bookmarks collection already exists in state it is reused (preserving its
     * server-side id / metadata); otherwise a local placeholder is created.
     */
    private fun buildBookmarksCollection(): Collection {
        val existing = _uiState.value.collections.items
            .firstOrNull { it.kind == CollectionKind.BOOKMARKS }

        return existing?.copy()
            ?: Collection(
                id = "bookmarks",
                userId = "user",
                kind = CollectionKind.BOOKMARKS,
                name = "Bookmarks",
                isPublic = false,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )
    }

    /**
     * Produces the merged [List<Collection>] shown in the UI.
     * Bookmarks always appear first; custom collections follow.
     *
     * Passing null for either argument preserves the currently stored value for that slot.
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

    private fun <T> reconcileObservedTotal(
        previousItems: List<T>,
        freshItems: List<T>,
        previousTotal: Int?,
        idSelector: (T) -> String,
    ): Int {
        val previousIds = previousItems.map(idSelector).toSet()
        val freshIds = freshItems.map(idSelector).toSet()
        val delta = freshIds.count { it !in previousIds } - previousIds.count { it !in freshIds }
        val baseline = previousTotal ?: previousItems.size
        return maxOf(baseline + delta, freshItems.size)
    }

    // -------------------------------------------------------------------------
    // Reset
    // -------------------------------------------------------------------------

    private fun resetAll() {
        likesObserverJob?.cancel(); likesObserverJob = null
        bookmarksObserverJob?.cancel(); bookmarksObserverJob = null
        collectionsObserverJob?.cancel(); collectionsObserverJob = null

        likesPagination.reset()
        bookmarksPagination.reset()
        collectionsPagination.reset()
        _uiState.value = UserProfileUiState()
    }
}