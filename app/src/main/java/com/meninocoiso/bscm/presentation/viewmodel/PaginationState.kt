package com.meninocoiso.bscm.presentation.viewmodel

/**
 * Tracks pagination cursor state for a single paginated list.
 */
data class PaginationState(
    val pageSize: Int,
    var currentPage: Int = 0,
    var isLoadingMore: Boolean = false,
    var hasMore: Boolean = true,
) {
    /** Offset to pass to the next API call (before calling [advancePage]). */
    fun nextOffset(): Int = currentPage * pageSize

    /** Call after a successful fetch to move to the next page. */
    fun advancePage() { currentPage++ }

    /** Resets to initial state, e.g. on pull-to-refresh or profile switch. */
    fun reset() {
        currentPage = 0
        isLoadingMore = false
        hasMore = true
    }
}