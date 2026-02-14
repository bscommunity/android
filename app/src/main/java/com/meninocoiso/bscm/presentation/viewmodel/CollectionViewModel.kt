package com.meninocoiso.bscm.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.repository.CollectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionViewModel @Inject constructor(
	private val collectionRepository: CollectionRepository
) : ViewModel() {
	// Paginated collections
	val collectionsState = PaginatedContentState(
		scope = viewModelScope,
		pageSize = 20,
		fetchPage = { limit, offset ->
			collectionRepository.getUserCollections(limit = limit, offset = offset)
		}
	)

	val collections = collectionsState.data
	val collectionsContentState = collectionsState.contentState
	val isLoadingMoreCollections = collectionsState.isLoadingMore

	// Paginated collection items
	private var currentCollectionId: String? = null
	val collectionItemsState = PaginatedContentState<CatalogItem>(
		scope = viewModelScope,
		pageSize = 20,
		fetchPage = { limit, offset ->
			val id = currentCollectionId
			if (id != null) {
				collectionRepository.getCollectionItems(collectionId = id, limit = limit, offset = offset)
			} else {
				Result.failure(IllegalStateException("No collection ID set"))
			}
		}
	)

	val collectionItems = collectionItemsState.data
	val collectionItemsContentState = collectionItemsState.contentState
	val isLoadingMoreItems = collectionItemsState.isLoadingMore
	val hasMoreItems = collectionItemsState.hasMore

	/**
	 * Fetch user collections with pagination
	 */
	fun fetchUserCollections(reset: Boolean = false) {
		if (reset) {
			collectionsState.loadInitial()
		} else {
			collectionsState.loadMore()
		}
	}

	/**
	 * Load items for a specific collection
	 */
	fun loadCollectionItems(collectionId: String, reset: Boolean = false) {
		if (reset || currentCollectionId != collectionId) {
			currentCollectionId = collectionId
			collectionItemsState.loadInitial()
		} else {
			collectionItemsState.loadMore()
		}
	}

	/**
	 * Create a new collection
	 */
	fun createCollection(name: String, isPublic: Boolean) {
		viewModelScope.launch {
			val result = collectionRepository.createCollection(name, isPublic)
			result.onSuccess { collection ->
				// Prepend new collection to the list
				collectionsState.updateData { listOf(collection) + it }
			}
		}
	}
}