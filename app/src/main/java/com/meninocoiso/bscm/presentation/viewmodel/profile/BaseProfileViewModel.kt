package com.meninocoiso.bscm.presentation.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.domain.result.ContentState
import com.meninocoiso.bscm.presentation.viewmodel.PaginationState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Shared UI building blocks
// ---------------------------------------------------------------------------

/**
 * Represents a paginated list of items plus its loading/error state.
 *
 * Analogy: imagine a "card stack" widget — it knows what cards are shown,
 * whether it's spinning, and whether there are more cards to pull.
 */
data class PagedSection<T>(
    val items: List<T> = emptyList(),
    val state: ContentState = ContentState.Loading,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
) {
    val isEmpty get() = items.isEmpty()
    val isIdle  get() = state != ContentState.Loading && !isRefreshing && !isLoadingMore
}

// ---------------------------------------------------------------------------
// Base ViewModel
// ---------------------------------------------------------------------------

/**
 * Base class that provides:
 *  - A reusable [fetchPaged] coroutine helper that handles all pagination bookkeeping.
 *  - A [SharedFlow] for one-shot snackbar messages.
 *
 * Subclasses only need to declare their own [MutableStateFlow] and call [fetchPaged].
 */
abstract class BaseProfileViewModel : ViewModel() {

    // One-shot events (e.g. snackbar messages) — SharedFlow so they're not replayed
    // on recomposition, unlike StateFlow.
    private val _snackbarEvents = MutableSharedFlow<String>()
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    protected fun emitSnackbar(message: String) {
        viewModelScope.launch { _snackbarEvents.emit(message) }
    }

    /**
     * Generic paginated fetch helper.
     *
     * Analogy: think of this as a universal "load more" button handler. You hand it:
     *  - the [pagination] bookmark so it knows where to start
     *  - a [fetch] lambda that calls the actual repository
     *  - [getItems] / [setSection] so it can read & write into your specific UI state
     *  - behaviour flags: [reset] (refresh) and [useCache]
     *
     * It handles the guard clauses, loading flags, page advancement, and error
     * branching — so subclasses never repeat that boilerplate.
     *
     * @param pagination        The [PaginationState] for this list.
     * @param reset             If true, resets pagination and replaces items (pull-to-refresh).
     * @param useCache          Hint passed to the repository. Ignored on explicit refreshes
     *                          ([reset] = true) for pages beyond the first.
     * @param showSkeletonWhen  Lambda evaluated to decide whether to switch state to Loading
     *                          (defaults to "when the list is empty").
     * @param fetch             Suspend lambda: receives (limit, offset, useCache) → Result<List<T>>.
     * @param getItems          Read the current item list from state.
     * @param setSection        Write an updated [PagedSection] back into state.
     * @param onFailureWithData Called instead of the error state when items already exist
     *                          (so users don't lose what they were looking at).
     */
    protected fun <T> fetchPaged(
        pagination: PaginationState,
        reset: Boolean = false,
        useCache: Boolean = true,
        showSkeletonWhen: () -> Boolean = { pagination.currentPage == 0 },
        fetch: suspend (limit: Int, offset: Int, useCache: Boolean) -> Result<List<T>>,
        getItems: () -> List<T>,
        setSection: (PagedSection<T>) -> Unit,
        onFailureWithData: (suspend () -> Unit)? = null,
    ) = viewModelScope.launch {
        // Guard: already loading or nothing left to load
        if (pagination.isLoadingMore && !reset) return@launch
        if (!reset && !pagination.hasMore) return@launch

        if (reset) pagination.reset()

        // Offset is calculated *before* advancePage so it points at the right window
        val offset = pagination.nextOffset()

        // Cache is only useful for page-0 reads; subsequent pages are always fresh
        val effectiveCache = useCache && offset == 0

        // Show skeleton only when the list is genuinely empty (first ever load)
        if (showSkeletonWhen()) {
            setSection(
                PagedSection(
                    items = getItems(),
                    state = ContentState.Loading,
                    isRefreshing = reset,
                )
            )
        }

        val isActuallyLoadingMore = !reset
        if (isActuallyLoadingMore) pagination.isLoadingMore = true
        setSection(
            PagedSection(
                items = getItems(),
                state = if (showSkeletonWhen()) ContentState.Loading else ContentState.Success,
                isRefreshing = reset,
                isLoadingMore = isActuallyLoadingMore,
                hasMore = pagination.hasMore,
            )
        )

        try {
            fetch(pagination.pageSize, offset, effectiveCache)
                .onSuccess { data ->
                    val merged = if (reset || offset == 0) data else getItems() + data
                    pagination.hasMore = data.size >= pagination.pageSize
                    pagination.advancePage()

                    setSection(
                        PagedSection(
                            items = merged,
                            state = ContentState.Success,
                            isRefreshing = false,
                            isLoadingMore = false,
                            hasMore = pagination.hasMore,
                        )
                    )
                }
                .onFailure { error ->
                    if (getItems().isNotEmpty()) {
                        // Preserve existing content; surface a snackbar instead
                        onFailureWithData?.invoke()
                            ?: emitSnackbar("Failed to load more: ${error.localizedMessage ?: "Unknown error"}")
                        setSection(
                            PagedSection(
                                items = getItems(),
                                state = ContentState.Success, // keep showing what we have
                                isRefreshing = false,
                                isLoadingMore = false,
                                hasMore = false,
                            )
                        )
                    } else {
                        setSection(
                            PagedSection(
                                items = emptyList(),
                                state = ContentState.Error,
                                isRefreshing = false,
                                isLoadingMore = false,
                                hasMore = false,
                            )
                        )
                    }
                }
        } finally {
            // Always release the lock — even on unexpected exceptions
            if (isActuallyLoadingMore) pagination.isLoadingMore = false
        }
    }

    /**
     * Convenience overload for pull-to-refresh: sets [isRefreshing] around the fetch,
     * then delegates to [fetchPaged] with [reset] = true.
     */
    protected fun <T> refreshPaged(
        pagination: PaginationState,
        useCache: Boolean = false,
        fetch: suspend (limit: Int, offset: Int, useCache: Boolean) -> Result<List<T>>,
        getItems: () -> List<T>,
        getSection: () -> PagedSection<T>,
        setSection: (PagedSection<T>) -> Unit,
        onFailureWithData: (suspend () -> Unit)? = null,
    ) = viewModelScope.launch {
        if (pagination.isLoadingMore) return@launch
        setSection(getSection().copy(isRefreshing = true))
        try {
            fetchPaged(
                pagination = pagination,
                reset = true,
                useCache = useCache,
                fetch = fetch,
                getItems = getItems,
                setSection = setSection,
                onFailureWithData = onFailureWithData,
            ).join() // wait for inner job so finally clears refreshing after data arrives
        } finally {
            setSection(getSection().copy(isRefreshing = false))
        }
    }
}