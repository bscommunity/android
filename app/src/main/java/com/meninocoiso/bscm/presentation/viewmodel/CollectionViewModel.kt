package com.meninocoiso.bscm.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.remote.ApiException
import com.meninocoiso.bscm.data.repository.ProfileCacheRepository
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Collection
import com.meninocoiso.bscm.domain.model.SimplifiedCollection
import com.meninocoiso.bscm.domain.model.toSimplifiedCollection
import com.meninocoiso.bscm.domain.repository.CollectionRepository
import com.meninocoiso.bscm.domain.result.ContentResult
import com.meninocoiso.bscm.domain.result.UiText
import com.meninocoiso.bscm.presentation.viewmodel.profile.BaseProfileViewModel
import com.meninocoiso.bscm.presentation.viewmodel.profile.PagedSection
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CollectionViewModel"

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val collectionRepository: CollectionRepository,
    private val profileCacheRepository: ProfileCacheRepository,
) : BaseProfileViewModel() {

    // -------------------------------------------------------------------------
    // UI state
    // -------------------------------------------------------------------------

    data class CollectionUiState(
        /** Items inside a specific open collection. */
        val items: PagedSection<CatalogItem> = PagedSection(),
        /** Cached per-type counters for CatalogFilters (charts, tourPasses, themes). */
        val itemCounts: Triple<Int, Int, Int> = Triple(0, 0, 0),
        /** The current user's own collections list (used by CollectionBottomSheet). */
        val userCollections: PagedSection<Collection> = PagedSection(),
        val isCreating: Boolean = false,
        val isUpdating: Boolean = false,
    )

    private val _uiState = MutableStateFlow(CollectionUiState())
    val uiState: StateFlow<CollectionUiState> = _uiState.asStateFlow()

    // -------------------------------------------------------------------------
    // Deep-link resolution — fetches a collection by ID and exposes it as
    // ContentResult so CollectionRoute can follow the same pattern as
    // ChartDetailsRoute.
    // -------------------------------------------------------------------------

    private val _collectionResult =
        MutableStateFlow<ContentResult<SimplifiedCollection>>(ContentResult.Loading)
    val collectionResult: StateFlow<ContentResult<SimplifiedCollection>> =
        _collectionResult.asStateFlow()

    // -------------------------------------------------------------------------
    // Pagination cursors
    // -------------------------------------------------------------------------

    private val itemsPagination = PaginationState(pageSize = 20)
    private val collectionsPagination = PaginationState(pageSize = 20)
    private var currentCollectionId: String? = null
    private var collectionMembershipObserverJob: Job? = null
    private var itemsFetchJob: Job? = null

    // -------------------------------------------------------------------------
    // Public API — user's own collections list (for CollectionBottomSheet)
    // -------------------------------------------------------------------------

    fun fetchUserCollections(reset: Boolean = false) = fetchPaged<Collection>(
        pagination = collectionsPagination,
        reset = reset,
        fetch = { limit, offset, cache ->
            val result = collectionRepository.getUserCollections(limit = limit, offset = offset, useCache = cache)
            result
        },
        getSection = { _uiState.value.userCollections },
        setSection = { section -> _uiState.update { it.copy(userCollections = section) } },
        onFailureWithData = { emitSnackbar(UiText.Res(R.string.failed_to_load_collections)) },
    )

    // -------------------------------------------------------------------------
    // Public API — items inside an open collection
    // -------------------------------------------------------------------------

    /**
     * Entry point called by [com.meninocoiso.bscm.presentation.screen.collection.CollectionScreen] whenever the collection changes or
     * a pull-to-refresh is triggered. Passing a new [collectionId] automatically
     * resets the cursor so stale data is never shown.
     */
    fun loadItems(collectionId: String, reset: Boolean = false) {
        if (itemsFetchJob?.isActive == true && collectionId == currentCollectionId) {
            return
        }

        val idChanged = collectionId != currentCollectionId
        if (idChanged || reset) {
            currentCollectionId = collectionId
            itemsPagination.reset()
            _uiState.update { it.copy(items = PagedSection(), itemCounts = Triple(0, 0, 0)) }
            hydrateCollectionItemCounts(collectionId)
            startCollectionMembershipObserver(collectionId)
        }

        Log.d(TAG, "Loading items for collection $collectionId (reset=$reset, idChanged=$idChanged)")

        itemsFetchJob = fetchPaged(
            pagination = itemsPagination,
            reset = reset || idChanged,
            fetch = { limit, offset, cache ->
                collectionRepository.getCollectionItems(
                    collectionId = collectionId,
                    limit = limit,
                    offset = offset,
                    useCache = cache,
                )
            },
            getSection = { _uiState.value.items },
            setSection = { section ->
                _uiState.update {
                    it.copy(items = section)
                }
            },
            onFailureWithData = { emitSnackbar(UiText.Res(R.string.failed_to_load_items)) },
        )
    }

    fun loadMoreItems(collectionId: String) {
        if (!_uiState.value.items.isLoadingMore && !_uiState.value.items.isRefreshing && _uiState.value.items.hasMore) {
            loadItems(collectionId)
        }
    }

    fun refreshItems(collectionId: String) = refreshPaged(
        pagination = itemsPagination,
        fetch = { limit, offset, cache ->
            collectionRepository.getCollectionItems(
                collectionId = collectionId,
                limit = limit,
                offset = offset,
                useCache = cache,
            )
        },
        getSection = { _uiState.value.items },
        setSection = { section ->
            _uiState.update {
                it.copy(items = section)
            }
            hydrateCollectionItemCounts(collectionId)
        },
        onFailureWithData = { emitSnackbar(UiText.Res(R.string.failed_to_update_collection_items)) },
    ).also { itemsFetchJob = it }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    fun fetchCollectionBySlug(username: String, slug: String) {
        viewModelScope.launch {
            _collectionResult.value = ContentResult.Loading
            collectionRepository.getCollectionBySlug(username, slug)
                .onSuccess { collection ->
                    Log.d(TAG, "Fetched collection: $collection")
                    if (collection.owner == null) {
                        Log.e(TAG, "Collection $username/$slug has no owner in response")
                        _collectionResult.value =
                            ContentResult.Error(UiText.Res(R.string.collection_data_incomplete_missing_owner))
                        return@onSuccess
                    }
                    _collectionResult.value =
                        ContentResult.Success(collection.toSimplifiedCollection())
                }
                .onFailure { e ->
                    Log.e(TAG, "Failed to fetch collection $username/$slug", e)
                    _collectionResult.value =
                        ContentResult.Error(
                            e.message?.let { UiText.Plain(it) } ?: UiText.Res(R.string.unknown_error),
                            e
                        )
                }
        }
    }

    /**
     * Creates a new collection and returns its ID on success, or throws ApiException on failure.
     * Prepends the new collection to the list optimistically.
     */
    suspend fun createCollection(name: String, isPublic: Boolean): String {
        _uiState.update { it.copy(isCreating = true) }
        try {
            return collectionRepository.createCollection(name, isPublic)
                .fold(
                    onSuccess = { collection ->
                        Log.d(TAG, "Created collection: ${collection.name} (${collection.id})")
                        // Optimistic prepend so the sheet reflects it immediately
                        _uiState.update { state ->
                            state.copy(
                                userCollections = state.userCollections.copy(
                                    items = listOf(collection) + state.userCollections.items
                                )
                            )
                        }

                        collection.id
                    },
                    onFailure = { e ->
                        Log.e(TAG, "Failed to create collection", e)
                        // Rethrow ApiException so callers can handle HTTP status-specific logic.
                        if (e is ApiException) throw e
                        // Wrap other exceptions into a generic ApiException with 500 status.
                        throw ApiException(HttpStatusCode.InternalServerError, e.message ?: "Unknown error")
                    }
                )
        } finally {
            _uiState.update { it.copy(isCreating = false) }
        }
    }

    /**
     * Updates an existing collection.
     */
    suspend fun updateCollection(collectionId: String, name: String?, isPublic: Boolean?): Result<String?> {
        _uiState.update { it.copy(isUpdating = true) }
        try {
            val result = collectionRepository.updateCollection(collectionId, name, isPublic)
            result.onSuccess {
                Log.d(TAG, "Updated collection: $collectionId")
                emitSnackbar(UiText.Res(R.string.collection_updated_successfully))
            }
            .onFailure { e ->
                Log.e(TAG, "Failed to update collection", e)
                emitSnackbar(UiText.Res(R.string.failed_to_update_collection))
            }
            return result
        } finally {
            _uiState.update { it.copy(isUpdating = false) }
        }
    }

    /**
     *  Deletes a collection by ID. On success, removes it from the user's collections list.
     */
    suspend fun deleteCollection(collectionId: String): Result<Unit> {
        try {
            val result = collectionRepository.deleteCollection(collectionId)
            result.onSuccess {
                Log.d(TAG, "Deleted collection: $collectionId")
                // Remove the deleted collection from the list
                _uiState.update { state ->
                    state.copy(
                        userCollections = state.userCollections.copy(
                            items = state.userCollections.items.filterNot { it.id == collectionId }
                        )
                    )
                }
                emitSnackbar(UiText.Res(R.string.collection_deleted_successfully))
            }
                .onFailure { e ->
                    Log.e(TAG, "Failed to delete collection", e)
                    emitSnackbar(UiText.Res(R.string.failed_to_delete_collection))
                }
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting collection", e)
            emitSnackbar(UiText.Res(R.string.failed_to_delete_collection))
            return Result.failure(e)
        }
    }

    private fun startCollectionMembershipObserver(collectionId: String) {
        collectionMembershipObserverJob?.cancel()
        collectionMembershipObserverJob = viewModelScope.launch {
            collectionRepository.observeCollectionChartIds(collectionId)
                .catch { e -> Log.e(TAG, "Collection membership observer error", e) }
                .collect { ids ->
                    val idSet = ids.toHashSet()
                    val current = _uiState.value.items

                    // Keep currently loaded items in sync with local membership mutations
                    // (e.g. remove-from-collection in details) without issuing a full refresh.
                    val filtered = current.items.filter { item ->
                        val key = item.id
                        key in idSet
                    }
                    val nextTotal = ids.size

                    if (filtered != current.items || current.total != nextTotal) {
                        _uiState.update { state ->
                            state.copy(
                                items = state.items.copy(
                                    items = filtered,
                                    total = nextTotal,
                                ),
                                itemCounts = state.itemCounts.copy(first = nextTotal)
                            )
                        }
                    }
                }
        }
    }

    private fun hydrateCollectionItemCounts(collectionId: String) {
        viewModelScope.launch {
            val cached = profileCacheRepository.getCollectionItemCounts(collectionId)?.toTriple()
                ?: Triple(0, 0, 0)
            _uiState.update { state ->
                state.copy(
                    itemCounts = state.itemCounts.copy(
                        second = cached.second,
                        third = cached.third,
                        first = if (state.itemCounts.first == 0) cached.first else state.itemCounts.first,
                    )
                )
            }
        }
    }
}