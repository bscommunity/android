package com.meninocoiso.bscm.presentation.viewmodel

import com.meninocoiso.bscm.domain.result.ContentState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Combines PaginatedDataSource with ContentState for a complete paginated content solution.
 * This handles both the data fetching and UI state management in one place.
 */
class PaginatedContentState<T>(
    scope: CoroutineScope,
    pageSize: Int = 20,
    fetchPage: suspend (limit: Int, offset: Int) -> Result<List<T>>
) {
    private val dataSource = PaginatedDataSource(scope, pageSize, fetchPage)

    private val _contentState = MutableStateFlow<ContentState>(ContentState.Loading)
    val contentState: StateFlow<ContentState> = _contentState.asStateFlow()

    val data: StateFlow<List<T>> = dataSource.data
    val isLoadingMore: StateFlow<Boolean> = dataSource.isLoadingMore
    val hasMore: StateFlow<Boolean> = dataSource.hasMore

    /**
     * Load initial data with proper state management
     */
    fun loadInitial() {
        _contentState.value = ContentState.Loading
        dataSource.loadInitial(
            onSuccess = { _contentState.value = ContentState.Success },
            onError = { _contentState.value = ContentState.Error }
        )
    }

    /**
     * Load more data (pagination)
     */
    fun loadMore() {
        dataSource.loadMore()
    }

    /**
     * Refresh the content
     */
    fun refresh() {
        _contentState.value = ContentState.Loading
        dataSource.refresh(
            onSuccess = { _contentState.value = ContentState.Success },
            onError = { _contentState.value = ContentState.Error }
        )
    }

    /**
     * Reset to initial state
     */
    fun reset() {
        dataSource.reset()
        _contentState.value = ContentState.Loading
    }

    /**
     * Update data without fetching
     */
    fun updateData(transform: (List<T>) -> List<T>) {
        dataSource.updateData(transform)
    }
}
