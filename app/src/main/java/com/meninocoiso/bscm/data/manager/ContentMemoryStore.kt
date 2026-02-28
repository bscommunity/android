package com.meninocoiso.bscm.data.manager

import com.meninocoiso.bscm.data.service.FeedOrchestrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Generic in-memory store that centralizes content and feed state management.
 * Delegates feed computation to [FeedOrchestrator] and applies updates atomically.
 */
class ContentMemoryStore<T> @Inject constructor(
    private val feedOrchestrator: FeedOrchestrator<T>
) {
    private val _contentById = MutableStateFlow<Map<String, T>>(emptyMap())
    val contentById: StateFlow<Map<String, T>> = _contentById.asStateFlow()

    private val _feedOrderIds = MutableStateFlow<List<String>>(emptyList())
    val feedOrderIds: StateFlow<List<String>> = _feedOrderIds.asStateFlow()

    private val _searchResultIds = MutableStateFlow<List<String>?>(null)
    val searchResultIds: StateFlow<List<String>?> = _searchResultIds.asStateFlow()

    fun replaceFeed(
        newContent: List<T>,
        getId: (T) -> String,
        isInstalled: (T) -> Boolean,
        onStaleRemove: (List<T>) -> Unit = {},
        coroutineScope: CoroutineScope
    ) {
        val result = feedOrchestrator.computeReplaceFeed(
            newContent = newContent,
            currentContent = _contentById.value,
            getId = getId,
            isInstalled = isInstalled
        )

        // Apply state changes atomically
        _contentById.update { current ->
            if (current == result.updatedContent) current else result.updatedContent
        }
        _feedOrderIds.update { current ->
            if (current == result.updatedFeedOrder) current else result.updatedFeedOrder
        }

        // Notify about stale content (database cleanup)
        if (result.staleContent.isNotEmpty()) {
            coroutineScope.launch {
                onStaleRemove(result.staleContent)
            }
        }
    }

    fun appendFeed(
        newContent: List<T>,
        getId: (T) -> String,
        isInstalled: (T) -> Boolean
    ) {
        if (newContent.isEmpty()) return

        val result = feedOrchestrator.computeAppendFeed(
            newContent = newContent,
            currentContent = _contentById.value,
            currentFeedOrder = _feedOrderIds.value,
            getId = getId,
            isInstalled = isInstalled
        )

        // Apply state changes atomically
        _contentById.update { current ->
            if (current == result.updatedContent) current else result.updatedContent
        }
        _feedOrderIds.update { current ->
            if (current == result.updatedFeedOrder) current else result.updatedFeedOrder
        }
    }

    fun addWithoutAffectingFeed(
        newContent: List<T>,
        getId: (T) -> String
    ) {
        if (newContent.isEmpty()) return

        val updatedMap = feedOrchestrator.computeUpsertContent(
            newContent = newContent,
            currentContent = _contentById.value,
            getId = getId
        )
        _contentById.update { current ->
            if (current == updatedMap) current else updatedMap
        }
    }

    fun upsertContent(items: List<T>, getId: (T) -> String) {
        if (items.isEmpty()) return
        val updatedMap = feedOrchestrator.computeUpsertContent(
            newContent = items,
            currentContent = _contentById.value,
            getId = getId
        )
        _contentById.update { current ->
            if (current == updatedMap) current else updatedMap
        }
    }

    fun setSearchResults(ids: List<String>) {
        _searchResultIds.update { current -> if (current == ids) current else ids }
    }

    fun clearSearchResults() {
        _searchResultIds.update { current -> if (current == null) current else null }
    }

    fun hasFeedItems(): Boolean = _feedOrderIds.value.isNotEmpty()

    fun currentFeedItems(): List<T> = _feedOrderIds.value.mapNotNull { _contentById.value[it] }
}