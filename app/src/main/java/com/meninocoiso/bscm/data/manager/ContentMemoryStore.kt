package com.meninocoiso.bscm.data.manager

import com.meninocoiso.bscm.data.service.FeedOrchestrator
import com.meninocoiso.bscm.domain.model.CatalogItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * In-memory store for catalog content with a single mutable source of truth.
 *
 * The only mutable state is [_content] (ordered list). All other views are derived
 * and updated automatically when [_content] changes.
 *
 * Public API:
 * - [content]: ordered list (single source of truth)
 * - [contentMap]: derived map for O(1) lookup by ID
 * - [searchResults]: derived list filtered by search IDs
 */
class ContentMemoryStore<T : CatalogItem> @Inject constructor(
    private val feedOrchestrator: FeedOrchestrator<T>
) {
    /**
     * SINGLE SOURCE OF TRUTH: Ordered list of items in feed/display order.
     *
     * This is the ONLY mutable state in this class. All updates flow through this one mutation point,
     * guaranteeing atomicity and consistency.
     *
     * Private mutable access prevents external modification; public read-only view via asStateFlow()
     * ensures consumers can only observe, not mutate.
     *
     * Order matters for:
     * - Display to users (respects sort/filter preferences)
     * - Pagination (offset-based queries depend on stable order)
     * - Feed state management (new items append, old items trim)
     */
    private val _content = MutableStateFlow<List<T>>(emptyList())
    val content: StateFlow<List<T>> = _content.asStateFlow()

    /**
     * DERIVED VIEW: Map-based index for O(1) lookup by ID.
     *
     * Automatically derived from [content] via [Flow.map].
     * Updated whenever [content] changes.
     * Created on-demand during observation (not stored in memory continuously).
     *
     * Usage: When you need fast ID-based lookup (e.g., findById operations)
     * Do NOT mutate this flow directly - it's read-only and derived.
     *
     * Example: val chart = contentMap.map { it["chart-id"] }
     */
    val contentMap: Flow<Map<String, T>> = content.map { list ->
        // Create map on-demand from ordered list
        // Kotlin's associateBy is optimized and very fast for <1000 items
        list.associateBy { it.id }
    }

    /**
     * FILTERED VIEW: IDs of items matching the current search query.
     *
     * This is a separate mutable state (not derived) because search results have their own
     * ordering that's independent from the main feed order.
     *
     * Private mutable access ensures only this class can update search state.
     * Consumers observe via [searchResults] flow which combines this with [content].
     *
     * Design rationale: Search results are a user action (query-based), not derived from
     * the feed itself. Storing search IDs separately allows efficient pagination and
     * clearing of search state without affecting the main feed.
     */
    private val _searchResultIds = MutableStateFlow<List<String>>(emptyList())

    /**
     * DERIVED VIEW: Search results as ordered list of items.
     *
     * Combines [_searchResultIds] with [content] to produce actual items in search order.
     * Returns empty list if no search is active (IDs empty).
     *
     * This is a derived flow: it automatically updates whenever either search IDs or
     * content changes. No manual synchronization needed.
     *
     * Visibility: Read-only public access via this Flow.
     * To update: use [setSearchResults] or [clearSearchResults] methods.
     */
    val searchResults: Flow<List<T>> = combine(_searchResultIds, content) { ids, items ->
        // If no search is active, return empty
        if (ids.isEmpty()) return@combine emptyList()

        // Create map for efficient lookup, then map IDs to items
        val itemMap = items.associateBy { it.id }
        ids.mapNotNull { id -> itemMap[id] }
    }

    /**
     * Replace the entire feed with new content.
     *
     * This operation:
     * 1. Replaces the current list with new content
     * 2. Removes stale non-installed items (via callback)
     * 3. Applies cache limits to prevent unbounded memory growth
     *
     * @param newContent The new items to set as the feed
     * @param getId Function to extract ID from item (for tracking)
     * @param isInstalled Function to check if item is installed (for retention)
     * @param onStaleRemove Callback when stale items are identified for deletion
     */
    fun replaceFeed(
        newContent: List<T>,
        getId: (T) -> String,
        isInstalled: (T) -> Boolean,
        onStaleRemove: (List<T>) -> Unit = {}
    ) {
        feedOrchestrator.replaceFeedContent(
            newContent = newContent,
            currentContent = content,
            updateContent = { _content.value = it },
            getId = getId,
            isInstalled = isInstalled,
            onStaleRemove = onStaleRemove,
        )
    }

    /**
     * Append new content to the existing feed.
     *
     * This operation:
     * 1. Adds new items to the end of current list
     * 2. Deduplicates items already in feed
     * 3. Applies cache limits if the list grows too large
     *
     * **Use case:** Pagination - fetching the next page of results
     * **Atomicity:** Single state mutation ensures consistency
     *
     * @param newContent Items to append to the feed
     * @param getId Function to extract unique ID from item
     * @param isInstalled Function to check if item is installed (for retention during limit)
     */
    fun appendFeed(
        newContent: List<T>,
        getId: (T) -> String,
        isInstalled: (T) -> Boolean
    ) {
        // Delegate to FeedOrchestrator which:
        // - Appends new items to existing list
        // - Deduplicates based on ID
        // - Applies cache limits
        feedOrchestrator.appendFeedContent(
            newContent = newContent,
            currentContent = content,
            updateContent = { _content.value = it },
            getId = getId,
            isInstalled = isInstalled
        )
    }

    /**
     * Add new content to the in-memory store WITHOUT affecting the main feed order.
     *
     * This operation only adds items to the store. They won't appear in the main feed view
     * unless explicitly added via [replaceFeed] or [appendFeed].
     *
     * **Use case:** Adding search results or supplementary content that shouldn't pollute the feed
     * **Atomicity:** Single state mutation (no race conditions)
     *
     * @param newContent Items to add to the store
     * @param getId Function to extract unique ID from item
     */
    fun addWithoutAffectingFeed(
        newContent: List<T>,
        getId: (T) -> String
    ) {
        // Delegate to FeedOrchestrator which upserts items into the list
        // without changing the order (items added at end or replaced in-place if exists)
        feedOrchestrator.addContentWithoutAffectingFeed(
            newContent = newContent,
            currentContent = content,
            updateContent = { _content.value = it },
            getId = getId
        )
    }

    /**
     * Upsert (update or insert) items into the feed.
     *
     * For each item:
     * - If ID already exists: replace existing item with new version
     * - If ID doesn't exist: append to end of list
     *
     * **Use case:** Updating existing items with new metadata (e.g., favorite status, version updates)
     * **Atomicity:** Single state mutation
     *
     * @param items Items to upsert
     * @param getId Function to extract unique ID from item
     */
    fun upsertContent(items: List<T>, getId: (T) -> String) {
        if (items.isEmpty()) return

        // Build a map of new items for efficient lookup
        val newItemsMap = items.associateBy(getId)

        // Update the list: replace existing items or append new ones
        val updated = _content.value.map { existing ->
            newItemsMap[getId(existing)] ?: existing
        }.toMutableList().apply {
            // Add items that weren't in the original list
            val existingIds = _content.value.map(getId).toSet()
            addAll(items.filter { getId(it) !in existingIds })
        }

        _content.value = updated
    }

    /**
     * Set the IDs of items that match the current search query.
     *
     * These IDs will be used to derive the [searchResults] flow, which combines them with
     * the current [content] to produce actual items.
     *
     * **Note:** The actual items must already exist in [content] for search results to display.
     * Search results are always a subset of current content.
     *
     * @param ids List of IDs that match the search query, in desired display order
     */
    fun setSearchResults(ids: List<String>) {
        _searchResultIds.value = ids
    }

    /**
     * Append new search result IDs to the existing search results.
     *
     * Used for pagination: when user scrolls in search results and requests the next page,
     * new IDs are appended to maintain the full result list.
     *
     * **Note:** Deduplicates to prevent showing the same item twice.
     *
     * @param ids New IDs to append to search results
     */
    fun appendSearchResults(ids: List<String>) {
        if (ids.isEmpty()) return
        val current = _searchResultIds.value
        val updated = (current + ids).distinct()
        _searchResultIds.value = updated
    }

    /**
     * Clear all search results.
     *
     * This causes [searchResults] flow to emit an empty list.
     * The main [content] feed remains unchanged.
     */
    fun clearSearchResults() {
        _searchResultIds.value = emptyList()
    }
}
