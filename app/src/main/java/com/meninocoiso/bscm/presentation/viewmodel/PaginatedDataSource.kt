package com.meninocoiso.bscm.presentation.viewmodel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Generic paginated data source that handles all the boilerplate for pagination.
 *
 * @param T The type of data being paginated
 * @param scope The coroutine scope to run operations in
 * @param pageSize The number of items per page
 * @param fetchPage Suspend function that fetches a page of data given limit and offset
 */
class PaginatedDataSource<T>(
    private val scope: CoroutineScope,
    private val pageSize: Int = 20,
    private val fetchPage: suspend (limit: Int, offset: Int) -> Result<List<T>>
) {
    private val _data = MutableStateFlow<List<T>>(emptyList())
    val data: StateFlow<List<T>> = _data.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private val _error = MutableStateFlow<Throwable?>(null)
    val error: StateFlow<Throwable?> = _error.asStateFlow()

    private var currentPage = 0
    private var isLoadingInternal = false

    /**
     * Load the first page of data
     */
    fun loadInitial(
        onSuccess: (List<T>) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        scope.launch {
            _isLoading.value = true
            _error.value = null
            currentPage = 0
            _hasMore.value = true
            isLoadingInternal = true

            val result = fetchPage(pageSize, 0)

            result.onSuccess { newData ->
                _data.value = newData
                _hasMore.value = newData.size >= pageSize
                currentPage = 1
                onSuccess(newData)
            }.onFailure { throwable ->
                _error.value = throwable
                onError(throwable)
            }

            _isLoading.value = false
            isLoadingInternal = false
        }
    }

    /**
     * Load the next page of data
     */
    fun loadMore(
        onSuccess: (List<T>) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        if (isLoadingInternal || !_hasMore.value) return

        scope.launch {
            _isLoadingMore.value = true
            isLoadingInternal = true

            val offset = currentPage * pageSize
            val result = fetchPage(pageSize, offset)

            result.onSuccess { newData ->
                _data.value = _data.value + newData
                _hasMore.value = newData.size >= pageSize
                currentPage++
                onSuccess(newData)
            }.onFailure { throwable ->
                _error.value = throwable
                onError(throwable)
            }

            _isLoadingMore.value = false
            isLoadingInternal = false
        }
    }

    /**
     * Refresh the data (reset to first page)
     */
    fun refresh(
        onSuccess: (List<T>) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) = loadInitial(onSuccess, onError)

    /**
     * Reset the data source to initial state
     */
    fun reset() {
        _data.value = emptyList()
        _isLoading.value = false
        _isLoadingMore.value = false
        _hasMore.value = true
        _error.value = null
        currentPage = 0
        isLoadingInternal = false
    }

    /**
     * Update data without fetching
     */
    fun updateData(transform: (List<T>) -> List<T>) {
        _data.value = transform(_data.value)
    }
}
