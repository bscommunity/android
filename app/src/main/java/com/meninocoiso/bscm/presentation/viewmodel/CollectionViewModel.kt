package com.meninocoiso.bscm.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Collection
import com.meninocoiso.bscm.domain.model.SimplifiedCollection
import com.meninocoiso.bscm.domain.repository.CollectionRepository
import com.meninocoiso.bscm.presentation.viewmodel.profile.BaseProfileViewModel
import com.meninocoiso.bscm.presentation.viewmodel.profile.PagedSection
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.meninocoiso.bscm.domain.model.toSimplifiedCollection
import com.meninocoiso.bscm.domain.result.ContentResult
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "CollectionViewModel"

@HiltViewModel
class CollectionViewModel @Inject constructor(
    private val collectionRepository: CollectionRepository,
) : BaseProfileViewModel() {

    // -------------------------------------------------------------------------
    // UI state
    // -------------------------------------------------------------------------

    data class CollectionUiState(
        /** Items inside a specific open collection. */
        val items: PagedSection<CatalogItem> = PagedSection(),
        /** The current user's own collections list (used by CollectionBottomSheet). */
        val userCollections: PagedSection<Collection> = PagedSection(),
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

    fun fetchCollectionById(collectionId: String?) {
        if (collectionId.isNullOrEmpty()) {
            _collectionResult.value = ContentResult.Error("Invalid collection ID")
            return
        }
        viewModelScope.launch {
            _collectionResult.value = ContentResult.Loading
            collectionRepository.getCollectionById(collectionId)
                .onSuccess { collection ->
                    _collectionResult.value =
                        ContentResult.Success(collection.toSimplifiedCollection())
                }
                .onFailure { e ->
                    Log.e(TAG, "Failed to fetch collection $collectionId", e)
                    _collectionResult.value =
                        ContentResult.Error(e.message ?: "Unknown error", e)
                }
        }
    }

    // -------------------------------------------------------------------------
    // Pagination cursors
    // -------------------------------------------------------------------------

    private val itemsPagination = PaginationState(pageSize = 20)
    private val collectionsPagination = PaginationState(pageSize = 20)
    private var currentCollectionId: String? = null

    // -------------------------------------------------------------------------
    // Public API — user's own collections list (for CollectionBottomSheet)
    // -------------------------------------------------------------------------

    fun fetchUserCollections(reset: Boolean = false) = fetchPaged(
        pagination = collectionsPagination,
        reset = reset,
        fetch = { limit, offset, cache ->
            collectionRepository.getUserCollections(limit = limit, offset = offset, useCache = cache)
        },
        getItems = { _uiState.value.userCollections.items },
        setSection = { section -> _uiState.update { it.copy(userCollections = section) } },
        onFailureWithData = { emitSnackbar("Falha ao carregar coleções") },
    )

    // -------------------------------------------------------------------------
    // Public API — items inside an open collection
    // -------------------------------------------------------------------------

    /**
     * Entry point called by [CollectionScreen] whenever the collection changes or
     * a pull-to-refresh is triggered. Passing a new [collectionId] automatically
     * resets the cursor so stale data is never shown.
     */
    fun loadItems(collectionId: String, reset: Boolean = false) {
        val idChanged = collectionId != currentCollectionId
        if (idChanged) {
            currentCollectionId = collectionId
            itemsPagination.reset()
            _uiState.update { it.copy(items = PagedSection()) }
        }

        fetchPaged(
            pagination = itemsPagination,
            reset = reset || idChanged,
            fetch = { limit, offset, _ ->
                collectionRepository.getCollectionItems(
                    collectionId = collectionId,
                    limit = limit,
                    offset = offset,
                )
            },
            getItems = { _uiState.value.items.items },
            setSection = { section -> _uiState.update { it.copy(items = section) } },
            onFailureWithData = { emitSnackbar("Falha ao carregar itens") },
        )
    }

    fun loadMoreItems(collectionId: String) {
        if (!_uiState.value.items.isLoadingMore && _uiState.value.items.hasMore) {
            loadItems(collectionId)
        }
    }

    fun refreshItems(collectionId: String) = refreshPaged(
        pagination = itemsPagination,
        fetch = { limit, offset, _ ->
            collectionRepository.getCollectionItems(
                collectionId = collectionId,
                limit = limit,
                offset = offset,
            )
        },
        getItems = { _uiState.value.items.items },
        getSection = { _uiState.value.items },
        setSection = { section -> _uiState.update { it.copy(items = section) } },
        onFailureWithData = { emitSnackbar("Falha ao atualizar coleção") },
    )

    // -------------------------------------------------------------------------
    // Public API — mutations
    // -------------------------------------------------------------------------

    /**
     * Creates a new collection and returns its ID on success, or null on failure.
     * Prepends the new collection to the list optimistically.
     */
    suspend fun createCollection(name: String, isPublic: Boolean): String? {
        return collectionRepository.createCollection(name, isPublic)
            .onSuccess { collection ->
                Log.d(TAG, "Created collection: ${collection.name} (${collection.id})")
                // Optimistic prepend so the sheet reflects it immediately
                _uiState.update { state ->
                    state.copy(
                        userCollections = state.userCollections.copy(
                            items = listOf(collection) + state.userCollections.items
                        )
                    )
                }
            }
            .onFailure {
                Log.e(TAG, "Failed to create collection", it)
                emitSnackbar("Falha ao criar coleção")
            }
            .getOrNull()?.id
    }
}

