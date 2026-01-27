package com.meninocoiso.bscm.data.manager

import com.meninocoiso.bscm.data.service.FeedOrchestrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Generic in-memory store that centralizes feed ordering and content map updates.
 * It delegates feed replacement/append logic (including cache limiting) to [FeedOrchestrator].
 */
class ContentMemoryStore<T> @Inject constructor(
    private val feedOrchestrator: FeedOrchestrator<T>
) {
    private val _content = MutableStateFlow<Map<String, T>>(emptyMap())
    val content: StateFlow<Map<String, T>> = _content.asStateFlow()

    private val _feedOrder = MutableStateFlow<List<String>>(emptyList())
    val feedOrder: StateFlow<List<String>> = _feedOrder.asStateFlow()

    private val _searchResults = MutableStateFlow<List<String>>(emptyList())
    val searchResults: StateFlow<List<String>> = _searchResults.asStateFlow()

    fun replaceFeed(
        newContent: List<T>,
        getId: (T) -> String,
        isInstalled: (T) -> Boolean,
        onStaleRemove: (List<T>) -> Unit = {},
        coroutineScope: CoroutineScope
    ) {
        feedOrchestrator.replaceFeedContent(
            newContent = newContent,
            contentMap = content,
            feedOrder = feedOrder,
            updateContentMap = { _content.value = it },
            updateFeedOrder = { _feedOrder.value = it },
            getId = getId,
            isInstalled = isInstalled,
            onStaleRemove = onStaleRemove,
            coroutineScope = coroutineScope
        )
    }

    fun appendFeed(
        newContent: List<T>,
        getId: (T) -> String,
        isInstalled: (T) -> Boolean
    ) {
        feedOrchestrator.appendFeedContent(
            newContent = newContent,
            contentMap = content,
            feedOrder = feedOrder,
            updateContentMap = { _content.value = it },
            updateFeedOrder = { _feedOrder.value = it },
            getId = getId,
            isInstalled = isInstalled
        )
    }

    fun addWithoutAffectingFeed(
        newContent: List<T>,
        getId: (T) -> String
    ) {
        feedOrchestrator.addContentWithoutAffectingFeed(
            newContent = newContent,
            contentMap = content,
            updateContentMap = { _content.value = it },
            getId = getId
        )
    }

    fun upsertContent(items: List<T>, getId: (T) -> String) {
        if (items.isEmpty()) return
        val updated = _content.value.toMutableMap().apply {
            items.forEach { put(getId(it), it) }
        }
        _content.value = updated
    }

    fun setSearchResults(ids: List<String>) {
        _searchResults.value = ids
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
    }
}

