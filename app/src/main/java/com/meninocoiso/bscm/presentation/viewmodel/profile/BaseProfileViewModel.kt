package com.meninocoiso.bscm.presentation.viewmodel.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.result.ContentState
import com.meninocoiso.bscm.domain.result.UiText
import com.meninocoiso.bscm.presentation.viewmodel.PaginationState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Shared UI building blocks
// ---------------------------------------------------------------------------

/**
 * Represents a paginated list of items plus its loading/error state.
 *
 * [total] is the server-reported grand total for this list (e.g. "247 likes").
 * It is populated from the first page response and preserved on subsequent pages.
 */
data class PagedSection<T>(
    val items: List<T> = emptyList(),
    val total: Int? = null,
    val counts: Triple<Int, Int, Int>? = null,
    val state: ContentState = ContentState.Loading,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = true,
) {
    val isEmpty get() = items.isEmpty()
    val isIdle  get() = state != ContentState.Loading && !isRefreshing && !isLoadingMore
}

// ---------------------------------------------------------------------------
// Fetch result — carries both items and the server-reported total
// ---------------------------------------------------------------------------

/**
 * What a paginated repository call must return.
 *
 * [total] is optional: repositories that don't (yet) return a total can omit it
 * and the existing [PagedSection.total] value will be preserved.
 */
data class PagedResult<T>(
    val items: List<T>,
    val total: Int? = null,
    /** (charts, tourPasses, themes) per-type counts reported by the server. */
    val counts: Triple<Int, Int, Int>? = null,
)

// ---------------------------------------------------------------------------
// Base ViewModel
// ---------------------------------------------------------------------------

/**
 * Base class that provides:
 *  - A reusable [fetchPaged] coroutine helper that handles all pagination bookkeeping.
 *  - A [SharedFlow] for one-shot snackbar messages.
 *
 * Subclasses only need to declare their own [kotlinx.coroutines.flow.MutableStateFlow] and call [fetchPaged].
 */
abstract class BaseProfileViewModel : ViewModel() {

    private val _snackbarEvents = MutableSharedFlow<UiText>()
    val snackbarEvents: SharedFlow<UiText> = _snackbarEvents.asSharedFlow()

    protected fun emitSnackbar(message: UiText) {
        viewModelScope.launch { _snackbarEvents.emit(message) }
    }

    /**
     * Generic paginated fetch helper.
     *
     * Analogy: think of this as a universal "load more" button handler. You hand it:
     *  - the [pagination] bookmark so it knows where to start
     *  - a [fetch] lambda that calls the actual repository, returning a [PagedResult]
     *  - [getSection] / [setSection] so it can read & write into your specific UI state
     *  - behaviour flags: [reset] (refresh) and [useCache]
     *
     * It handles guard clauses, loading flags, page advancement, total count
     * preservation, and error branching — so subclasses never repeat that boilerplate.
     *
     * @param pagination        The [PaginationState] for this list.
     * @param reset             If true, resets pagination and replaces items (pull-to-refresh).
     * @param useCache          Hint passed to the repository. Always false for pages beyond first.
     * @param showSkeletonWhen  Lambda evaluated to decide whether to switch state to Loading.
     *                          Defaults to "when the list is empty".
     * @param fetch             Suspend lambda: (limit, offset, useCache) → Result<PagedResult<T>>.
     * @param getSection        Read the current [PagedSection] from state.
     * @param setSection        Write an updated [PagedSection] back into state.
     * @param onFailureWithData Called instead of the error state when items already exist,
     *                          so users don't lose what they were looking at.
     */
    protected fun <T> fetchPaged(
        pagination: PaginationState,
        reset: Boolean = false,
        useCache: Boolean = true,
        showSkeletonWhen: () -> Boolean = { pagination.currentPage == 0 },
        fetch: suspend (limit: Int, offset: Int, useCache: Boolean) -> Result<PagedResult<T>>,
        getSection: () -> PagedSection<T>,
        setSection: (PagedSection<T>) -> Unit,
        onFailureWithData: (suspend () -> Unit)? = null,
    ): Job = viewModelScope.launch {
        // Guard: already loading or nothing left to load
        if (pagination.isLoadingMore && !reset) return@launch
        if (!reset && !pagination.hasMore) return@launch

        if (reset) pagination.reset()

        val offset = pagination.nextOffset()
        val effectiveCache = useCache && offset == 0
        val isLoadingMore = !reset && offset > 0
        val showSkeleton = showSkeletonWhen()

        // Single pre-fetch state update
        setSection(
            getSection().copy(
                state = if (showSkeleton) ContentState.Loading else ContentState.Success,
                isLoadingMore = isLoadingMore,
            )
        )

        if (isLoadingMore) pagination.isLoadingMore = true

        try {
            fetch(pagination.pageSize, offset, effectiveCache)
                .onSuccess { result ->
                    val merged = if (reset || offset == 0) result.items
                    else getSection().items + result.items

                    pagination.hasMore = result.items.size >= pagination.pageSize
                    pagination.advancePage()

                    setSection(
                        PagedSection(
                            items = merged,
                            // Preserve existing total/counts if the new page didn't return one
                            total = result.total ?: getSection().total,
                            counts = result.counts ?: getSection().counts,
                            state = ContentState.Success,
                            isRefreshing = false,
                            isLoadingMore = false,
                            hasMore = pagination.hasMore,
                        )
                    )
                }
                .onFailure { error ->
                    val current = getSection()
                    if (current.items.isNotEmpty()) {
                        onFailureWithData?.invoke()
                            ?: emitSnackbar(
                                UiText.Res(
                                    R.string.failed_to_load_more,
                                    error.localizedMessage ?: "Unknown error"
                                )
                            )
                        // Preserve existing content; surface a snackbar instead of an error screen
                        setSection(
                            current.copy(
                                state = ContentState.Success,
                                isRefreshing = false,
                                isLoadingMore = false,
                                hasMore = false,
                            )
                        )
                    } else {
                        setSection(
                            PagedSection(
                                state = ContentState.Error,
                                isRefreshing = false,
                                isLoadingMore = false,
                                hasMore = false,
                            )
                        )
                    }
                }
        } finally {
            if (isLoadingMore) pagination.isLoadingMore = false
        }
    }

    /**
     * Convenience overload for pull-to-refresh.
     *
     * Sets the refreshing flag immediately so the UI shows a spinner, then delegates
     * to [fetchPaged] with `reset = true`. The refreshing flag is cleared in
     * [fetchPaged] itself on both success and failure paths, so there's no
     * second `finally` layer that could race with the inner job.
     */
    protected fun <T> refreshPaged(
        pagination: PaginationState,
        useCache: Boolean = false,
        fetch: suspend (limit: Int, offset: Int, useCache: Boolean) -> Result<PagedResult<T>>,
        getSection: () -> PagedSection<T>,
        setSection: (PagedSection<T>) -> Unit,
        onFailureWithData: (suspend () -> Unit)? = null,
    ): Job {
        if (pagination.isLoadingMore) return viewModelScope.launch {}
        setSection(getSection().copy(isRefreshing = true))
        return fetchPaged(
            pagination = pagination,
            reset = true,
            useCache = useCache,
            showSkeletonWhen = { getSection().items.isEmpty() },
            fetch = fetch,
            getSection = getSection,
            setSection = setSection,
            onFailureWithData = onFailureWithData,
        )
    }
}