package com.meninocoiso.bscm.data.manager

import android.util.Log
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.enums.OperationOption
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.repository.ContentAnalyticsRepository
import com.meninocoiso.bscm.domain.repository.ContentFeedRepository
import com.meninocoiso.bscm.domain.repository.ContentItemRepository
import com.meninocoiso.bscm.domain.repository.ContentLocalRepository
import com.meninocoiso.bscm.domain.repository.ContentOperationPolicy
import com.meninocoiso.bscm.domain.repository.ContentQuery
import com.meninocoiso.bscm.domain.repository.ContentSuggestionsRepository
import com.meninocoiso.bscm.domain.result.ContentResult
import com.meninocoiso.bscm.domain.result.ContentState
import com.meninocoiso.bscm.domain.result.UiText
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
class ContentManager<T : CatalogItem, S, Q : ContentQuery> @Inject constructor(
    private val remoteRepository: ContentFeedRepository<T, S, Q>,
    private val localRepository: ContentLocalRepository<T, S, Q>,
    private val remoteItemRepository: ContentItemRepository<T>,
    private val localItemRepository: ContentItemRepository<T>,
    private val operationPolicy: ContentOperationPolicy<T>,
    private val suggestionsRepository: ContentSuggestionsRepository,
    private val analyticsRepository: ContentAnalyticsRepository,
    private val memoryStore: ContentMemoryStore<T>,
    private val coroutineScope: CoroutineScope,
    private val chartStateMerger: ChartStateMerger,
) {

    @Suppress("UNCHECKED_CAST")
    private suspend fun mergeRemoteWithLocalDeviceState(items: List<T>): List<T> {
        if (items.isEmpty()) return items

        if (items.all { it is Chart }) {
            return chartStateMerger.mergeRemoteCharts(items.filterIsInstance<Chart>()) as List<T>
        }

        return items.map { incoming ->
            val existing = memoryStore.contentById.value[incoming.id]
            if (incoming is Chart && existing is Chart) {
                // Device install status is local-only state and must survive remote refreshes.
                val mergedInstalled = if (existing.isInstalled == true) true else incoming.isInstalled
                incoming.copy(isInstalled = mergedInstalled) as T
            } else {
                incoming
            }
        }
    }

    private val _cacheState = MutableStateFlow<ContentState>(ContentState.Loading)
    val cacheState: StateFlow<ContentState> = _cacheState.asStateFlow()

    private val _feedState = MutableStateFlow<ContentState>(ContentState.Loading)
    val feedState: StateFlow<ContentState> = _feedState.asStateFlow()

    val feedContent: Flow<List<T>> = memoryStore.feedOrderIds.combineWith(memoryStore.contentById)
    val installedContent: Flow<List<T>> =
        memoryStore.contentById.mapValuesList { it.isInstalled == true }
    val searchContent: Flow<List<T>> =
        memoryStore.searchResultIds.combineWithNullable(memoryStore.contentById)

    fun updateCacheState(newState: ContentState) {
        _cacheState.value = newState
    }

    fun updateFeedState(newState: ContentState) {
        _feedState.value = newState
    }

    fun getItem(id: String): Flow<ContentResult<T>> = flow {
        emit(ContentResult.Loading)

        val localResult = localItemRepository.getItem(id).first()
        localResult.fold(
            onSuccess = { item ->
                memoryStore.upsertContent(listOf(item)) { it.id }
                emit(ContentResult.Success(item))
            },
            onFailure = {
                val remoteResult = remoteItemRepository.getItem(id).first()
                remoteResult.fold(
                    onSuccess = { item ->
                        memoryStore.upsertContent(listOf(item)) { it.id }
                        coroutineScope.launch { localRepository.insert(listOf(item)).first() }
                        emit(ContentResult.Success(item))
                    },
                    onFailure = { err ->
                        emit(
                            ContentResult.Error(
                                err.message?.let { UiText.Plain(it) }
                                    ?: UiText.Res(R.string.content_not_found),
                                err
                            )
                        )
                    }
                )
            }
        )
    }

    fun getItemsById(ids: List<String>): Flow<ContentResult<List<T>>> = flow {
        emit(ContentResult.Loading)
        val cachedItems = ids.mapNotNull { memoryStore.contentById.value[it] }
        if (cachedItems.size == ids.size) {
            emit(ContentResult.Success(cachedItems))
            return@flow
        }

        val localResult = localItemRepository.getItems(ids).first()
        localResult.fold(
            onSuccess = { items ->
                Log.d("ContentManager", "Fetched ${items.size} items from local DB for IDs: $ids")
                memoryStore.upsertContent(items) { it.id }
                emit(ContentResult.Success(items))
            },
            onFailure = {
                val remoteResult = remoteItemRepository.getItems(ids).first()
                remoteResult.fold(
                    onSuccess = { items ->
                        Log.d(
                            "ContentManager",
                            "Fetched ${items.size} items from remote for IDs: $ids"
                        )
                        memoryStore.upsertContent(items) { it.id }
                        coroutineScope.launch { localRepository.insert(items).first() }
                        emit(ContentResult.Success(items))
                    },
                    onFailure = { err ->
                        emit(
                            ContentResult.Error(
                                err.message?.let { UiText.Plain(it) }
                                    ?: UiText.Res(R.string.content_not_found),
                                err
                            )
                        )
                    }
                )
            }
        )
    }

    fun getSuggestions(query: String): Flow<List<String>> = flow {
        if (query.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val result = suggestionsRepository.getSuggestions(query).first()
        emit(result.getOrElse { emptyList() })
    }

    suspend fun postAnalytics(id: String, operation: OperationOption): Flow<Result<Boolean>> =
        analyticsRepository.postAnalytics(id, operation)

    suspend fun updateContent(
        id: String,
        operation: OperationOption,
    ): ContentResult<T> {
        val existing = memoryStore.contentById.value[id]
            ?: localItemRepository.getItem(id).first().getOrElse { err ->
                return ContentResult.Error(
                    err.message?.let { UiText.Plain(it) }
                        ?: UiText.Res(R.string.content_not_found),
                    err
                )
            }

        val updated = operationPolicy.apply(existing, operation).getOrElse { err ->
            return ContentResult.Error(
                err.message?.let { UiText.Plain(it) }
                    ?: UiText.Res(R.string.failed_to_update),
                err
            )
        }

        val dbResult = localRepository.update(listOf(updated)).first()
        if (dbResult.isFailure || dbResult.getOrNull() != true) {
            return ContentResult.Error(
                UiText.Res(R.string.failed_to_update),
                dbResult.exceptionOrNull()
            )
        }

        memoryStore.upsertContent(listOf(updated)) { it.id }
        return ContentResult.Success(updated)
    }

    suspend fun loadCachedContent(sortBy: S, limit: Int? = null, filters: Q? = null) {
        _cacheState.value = ContentState.Loading
        try {
            val cached = localRepository.getContent(
                query = null,
                sortBy = sortBy,
                limit = limit,
                filters = filters
            ).first()
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
        sortBy: S,
        forceRefresh: Boolean = false,
        limit: Int = 10,
        offset: Int = 0,
        filters: Q? = null,
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
        val remoteResult = remoteRepository.getContent(
            query = null,
            sortBy = sortBy,
            limit = limit,
            offset = offset,
            filters = filters
        ).first()
        remoteResult.fold(
            onSuccess = { remoteItems ->
                val items = mergeRemoteWithLocalDeviceState(remoteItems)
                Log.d("ContentManager", "Fetched ${items.size} items from remote")
                if (offset == 0) {
                    memoryStore.replaceFeed(
                        newContent = items,
                        getId = { it.id },
                        isInstalled = { it.isInstalled == true },
                        // no onStaleRemove
                        coroutineScope = coroutineScope
                    )
                } else {
                    memoryStore.appendFeed(
                        items,
                        getId = { it.id },
                        isInstalled = { it.isInstalled == true })
                }
                coroutineScope.launch { localRepository.update(items).first() }
                _feedState.value = ContentState.Success
                emit(ContentResult.Success(items))
            },
            onFailure = { err ->
                _feedState.value = ContentState.Error
                emit(
                    ContentResult.Error(
                        err.message?.let { UiText.Plain(it) }
                            ?: UiText.Res(R.string.failed_to_fetch_feed_charts),
                        err
                    )
                )
            }
        )
    }.catch { e ->
        _feedState.value = ContentState.Error
        emit(ContentResult.Error(UiText.Res(R.string.failed_to_fetch_feed_charts), e))
    }

    fun search(
        query: String,
        sortBy: S? = null,
        limit: Int = 10,
        offset: Int = 0,
        filters: Q? = null,
    ): Flow<ContentResult<List<T>>> = flow {
        if (query.isBlank()) {
            memoryStore.clearSearchResults()
            emit(ContentResult.Success(emptyList()))
            return@flow
        }
        emit(ContentResult.Loading)
        val remoteResult = remoteRepository.getContent(
            query = query,
            sortBy = sortBy,
            limit = limit,
            offset = offset,
            filters = filters
        ).first()
        remoteResult.fold(
            onSuccess = { remoteItems ->
                val items = mergeRemoteWithLocalDeviceState(remoteItems)
                memoryStore.addWithoutAffectingFeed(items, getId = { it.id })
                val newIds =
                    if (offset == 0) items.map { it.id } else memoryStore.searchResultIds.value?.plus(
                        items.map { it.id }) ?: items.map { it.id }
                memoryStore.setSearchResults(newIds)
                Log.d("ContentManager", "Search for '$query' returned ${items.size} items")
                emit(ContentResult.Success(items))
            },
            onFailure = { err ->
                emit(
                    ContentResult.Error(
                        UiText.Res(R.string.search_failed),
                        err
                    )
                )
            }
        )
    }.catch { e -> emit(ContentResult.Error(UiText.Res(R.string.search_failed), e)) }
}

// -- small collection helpers
private fun <T> StateFlow<List<String>>.combineWith(
    contentFlow: StateFlow<Map<String, T>>
): Flow<List<T>> = kotlinx.coroutines.flow.combine(this, contentFlow) { order, map ->
    if (order.isEmpty()) map.values.toList() else order.mapNotNull { map[it] }
}

// Nullable variant: null order = no active search (empty), empty list = 0 results (empty)
private fun <T> StateFlow<List<String>?>.combineWithNullable(
    contentFlow: StateFlow<Map<String, T>>
): Flow<List<T>> = kotlinx.coroutines.flow.combine(this, contentFlow) { order, map ->
    order?.mapNotNull { map[it] } ?: emptyList()
}

private fun <T> StateFlow<Map<String, T>>.mapValuesList(predicate: (T) -> Boolean): Flow<List<T>> =
    this.map { values -> values.values.filter(predicate) }
