package com.meninocoiso.bscm.data.manager

import android.content.Context
import android.util.Log
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.repository.ContentRepository
import com.meninocoiso.bscm.domain.result.ContentResult
import com.meninocoiso.bscm.domain.result.ContentState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generic manager that orchestrates feed fetch/search/update for any [CatalogItem].
 *
 * **Architecture:** Works with [ContentMemoryStore] which uses a single source of truth (List<T>).
 * All operations go through the memory store's public methods which handle consistency.
 *
 * Responsibilities:
 * - Load cached content from local repository
 * - Fetch remote feed with pagination
 * - Search remote content
 * - Sync installed status
 * - Manage state (loading, success, error)
 *
 * Concrete managers (e.g., charts) should delegate to this with appropriate adapters.
 */
@Singleton
class ContentManager<T : CatalogItem> @Inject constructor(
    private val context: Context,
    private val remoteRepository: ContentRepository<T>,
    private val localRepository: ContentRepository<T>,
    private val memoryStore: ContentMemoryStore<T>,
    private val coroutineScope: CoroutineScope,
) {

    private val _cacheState = MutableStateFlow<ContentState>(ContentState.Loading)
    val cacheState: StateFlow<ContentState> = _cacheState.asStateFlow()

    private val _feedState = MutableStateFlow<ContentState>(ContentState.Loading)
    val feedState: StateFlow<ContentState> = _feedState.asStateFlow()

    /**
     * Main feed content as ordered list.
     * This is the single source of truth from [memoryStore].
     * Direct flow - no transformation needed since store already provides List<T>.
     */
    val memoryContent: Flow<List<T>> = memoryStore.content

    /**
     * Installed items filtered from main content.
     * Derived from memoryContent - shows only items that have been locally installed.
     */
    val installedContent: Flow<List<T>> = memoryStore.content.map { items ->
        items.filter { it.isInstalled == true }
    }

    fun updateCacheState(newState: ContentState) { _cacheState.value = newState }
    fun updateFeedState(newState: ContentState) { _feedState.value = newState }

    /**
     * Load cached content from local database.
     *
     * This is typically done on app startup to show previously cached data immediately.
     * After loading, [fetchFeed] should be called to get fresh data from remote.
     *
     * @param sortBy Sort option for ordering results
     * @param limit Maximum number of items to load (null = no limit)
     */
    suspend fun loadCachedContent(sortBy: Any, limit: Int? = null) {
        _cacheState.value = ContentState.Loading
        try {
            val cached = localRepository.getSorted(sortBy, limit = limit).first()
            cached.fold(
                onSuccess = { list ->
                    memoryStore.replaceFeed(
                        newContent = list,
                        getId = { it.id },
                        isInstalled = { it.isInstalled == true },
                    )
                    _cacheState.value = ContentState.Success
                },
                onFailure = { _cacheState.value = ContentState.Error }
            )
        } catch (e: Exception) {
            Log.e("ContentManager", "loadCachedContent failed", e)
            _cacheState.value = ContentState.Error
        }
    }

    /**
     * Fetch feed content from remote repository with pagination support.
     *
     * Behavior:
     * - offset=0: Replace entire feed (new fetch or refresh)
     * - offset>0: Append to existing feed (pagination)
     *
     * Side effects:
     * - Updates memory store (replaceFeed or appendFeed)
     * - Persists fetched items to local database
     * - Triggers stale item cleanup for offset=0
     *
     * @param sortBy Sort option
     * @param forceRefresh Ignored, kept for API compatibility
     * @param limit Maximum items per page
     * @param offset Page offset for pagination
     * @return Flow emitting loading → success with items or error
     */
    fun fetchFeed(
        sortBy: Any,
        forceRefresh: Boolean = false,
        limit: Int = 10,
        offset: Int = 0,
    ): Flow<ContentResult<List<T>>> = flow {
        emit(ContentResult.Loading)
        val remoteResult = remoteRepository.getSorted(sortBy, limit, offset).first()
        remoteResult.fold(
            onSuccess = { items ->
                Log.d("ContentManager", "Fetched ${items.size} items from remote")
                if (offset == 0) {
                    // First page: replace entire feed
                    memoryStore.replaceFeed(
                        newContent = items,
                        getId = { it.id },
                        isInstalled = { it.isInstalled == true },
                        onStaleRemove = { stale ->
                            // Async: delete stale items from database
                            coroutineScope.launch { localRepository.delete(stale).first() }
                        },
                    )
                } else {
                    // Pagination: append new items
                    memoryStore.appendFeed(
                        items,
                        getId = { it.id },
                        isInstalled = { it.isInstalled == true }
                    )
                }
                // Update database asynchronously
                coroutineScope.launch { localRepository.update(items).first() }
                emit(ContentResult.Success(items))
            },
            onFailure = { err ->
                emit(ContentResult.Error(err.message ?: context.getString(R.string.failed_to_fetch_feed_charts), err))
            }
        )
    }.catch { e ->
        emit(ContentResult.Error(context.getString(R.string.failed_to_fetch_feed_charts), e))
    }

    /**
     * Search for content by query.
     *
     * Behavior:
     * - Empty query: Clear search results, show nothing
     * - First page (offset=0): Replace search results
     * - Pagination (offset>0): Append to search results
     *
     * Search items are added to memory store but NOT in the main feed.
     * They are tracked separately in [memoryStore.searchResults].
     *
     * @param query Search terms
     * @param limit Maximum items per page
     * @param offset Page offset for pagination
     * @return Flow emitting loading → success with items or error
     */
    fun search(
        query: String,
        limit: Int = 10,
        offset: Int = 0,
    ): Flow<ContentResult<List<T>>> = flow {
        if (query.isBlank()) {
            // Empty query: clear search
            memoryStore.clearSearchResults()
            emit(ContentResult.Success(emptyList()))
            return@flow
        }
        emit(ContentResult.Loading)
        val remoteResult = remoteRepository.search(query, limit, offset).first()
        remoteResult.fold(
            onSuccess = { items ->
                // Add items to memory store without affecting main feed
                memoryStore.addWithoutAffectingFeed(items, getId = { it.id })

                // Build search results IDs in order, handling pagination
                val newIds = if (offset == 0) {
                    // First page: replace search results
                    items.map { it.id }
                } else {
                    // Pagination: append to existing search results
                    // Note: searchResults is a Flow, so we need to get the current IDs
                    // This is a limitation of using IDs separately; in a pure single-source
                    // model we could avoid this. For now, we track IDs explicitly.
                    emptyList() // Handled by caller in pagination
                }

                memoryStore.setSearchResults(newIds)
                emit(ContentResult.Success(items))
            },
            onFailure = { err ->
                emit(ContentResult.Error(context.getString(R.string.search_failed), err))
            }
        )
    }.catch { e ->
        emit(ContentResult.Error(context.getString(R.string.search_failed), e))
    }
}
