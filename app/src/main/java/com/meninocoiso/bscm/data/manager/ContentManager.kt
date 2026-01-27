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

    val feedContent: Flow<List<T>> = memoryStore.feedOrderIds.combineWith(memoryStore.contentById)
    val installedContent: Flow<List<T>> = memoryStore.contentById.mapValuesList { it.isInstalled == true }

    fun updateCacheState(newState: ContentState) { _cacheState.value = newState }
    fun updateFeedState(newState: ContentState) { _feedState.value = newState }

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
                        coroutineScope = coroutineScope
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

    fun fetchFeed(
        sortBy: Any,
        forceRefresh: Boolean = false,
        limit: Int = 10,
        offset: Int = 0,
    ): Flow<ContentResult<List<T>>> = flow {
        // We leave this to the caller (e.g WorkshopViewModel) to set before invoking fetch
        // _feedState.value = ContentState.Loading

        if (!forceRefresh && offset == 0 && memoryStore.hasFeedItems()) {
            val cachedFeed = memoryStore.currentFeedItems()
            if (cachedFeed.isNotEmpty()) {
                _feedState.value = ContentState.Success
                emit(ContentResult.Success(cachedFeed))
                return@flow
            }
        }

        emit(ContentResult.Loading)
        val remoteResult = remoteRepository.getSorted(sortBy, limit, offset).first()
        remoteResult.fold(
            onSuccess = { items ->
                Log.d("ContentManager", "Fetched ${items.size} items from remote")
                if (offset == 0) {
                    memoryStore.replaceFeed(
                        newContent = items,
                        getId = { it.id },
                        isInstalled = { it.isInstalled == true },
                        onStaleRemove = { stale ->
                            coroutineScope.launch { localRepository.delete(stale).first() }
                        },
                        coroutineScope = coroutineScope
                    )
                } else {
                    memoryStore.appendFeed(items, getId = { it.id }, isInstalled = { it.isInstalled == true })
                }
                coroutineScope.launch { localRepository.update(items).first() }
                _feedState.value = ContentState.Success
                emit(ContentResult.Success(items))
            },
            onFailure = { err ->
                _feedState.value = ContentState.Error
                emit(ContentResult.Error(err.message ?: context.getString(R.string.failed_to_fetch_feed_charts), err))
            }
        )
    }.catch { e ->
        _feedState.value = ContentState.Error
        emit(ContentResult.Error(context.getString(R.string.failed_to_fetch_feed_charts), e))
    }

    fun search(
        query: String,
        limit: Int = 10,
        offset: Int = 0,
    ): Flow<ContentResult<List<T>>> = flow {
        if (query.isBlank()) {
            memoryStore.clearSearchResults()
            emit(ContentResult.Success(emptyList()))
            return@flow
        }
        emit(ContentResult.Loading)
        val remoteResult = remoteRepository.search(query, limit, offset).first()
        remoteResult.fold(
            onSuccess = { items ->
                memoryStore.addWithoutAffectingFeed(items, getId = { it.id })
                val newIds = if (offset == 0) items.map { it.id } else memoryStore.searchResultIds.value + items.map { it.id }
                memoryStore.setSearchResults(newIds)
                emit(ContentResult.Success(items))
            },
            onFailure = { err -> emit(ContentResult.Error(context.getString(R.string.search_failed), err)) }
        )
    }.catch { e -> emit(ContentResult.Error(context.getString(R.string.search_failed), e)) }
}

// -- small collection helpers
private fun <T> StateFlow<List<String>>.combineWith(
    contentFlow: StateFlow<Map<String, T>>
): Flow<List<T>> = kotlinx.coroutines.flow.combine(this, contentFlow) { order, map ->
    if (order.isEmpty()) map.values.toList() else order.mapNotNull { map[it] }
}

private fun <T> StateFlow<Map<String, T>>.mapValuesList(predicate: (T) -> Boolean): Flow<List<T>> =
    this.map { values -> values.values.filter(predicate) }
