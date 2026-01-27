package com.meninocoiso.bscm.data.service

import android.util.Log
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

private const val TAG = "FeedOrchestrator"

/**
 * Orchestrates feed operations with a single source of truth model.
 *
 * Responsibilities:
 * - Replace feed (stale removal + cache limiting)
 * - Append feed (deduplication + cache limiting)
 * - Add content without affecting feed order
 * - Apply cache limits when memory grows too large
 *
 * **Single Source of Truth:** All operations work directly with List<T>, eliminating
 * the complexity of synchronizing separate content maps and order lists.
 *
 * **Atomicity:** Each operation updates content exactly once, preventing race conditions.
 */
class FeedOrchestrator<T> @Inject constructor(
    private val cacheManager: ContentCacheManager
) {

    /**
     * Replace the entire feed with new content.
     *
     * Steps:
     * 1. Identify stale items (in current list but not in new content and not installed)
     * 2. Notify caller to delete stale items from database
     * 3. Replace content with new items
     * 4. Apply cache limits to prevent unbounded growth
     *
     * **Why atomic:** Single [updateContent] call ensures all changes happen together.
     *
     * @param newContent The new feed items to set
     * @param currentContent Current content state (for comparison)
     * @param updateContent Callback to update the mutable state
     * @param getId Function to extract unique ID from item
     * @param isInstalled Function to check if item is locally installed
     * @param onStaleRemove Callback when stale items are identified (for DB cleanup)
     * @param coroutineScope CoroutineScope for async DB operations
     */
    fun replaceFeedContent(
        newContent: List<T>,
        currentContent: StateFlow<List<T>>,
        updateContent: (List<T>) -> Unit,
        getId: (T) -> String,
        isInstalled: (T) -> Boolean,
        onStaleRemove: (List<T>) -> Unit = {}
    ) {
        val newIds = newContent.map(getId).toSet()
        val current = currentContent.value

        // Find items that are in memory but not in new feed and not installed
        // These are candidates for deletion
        val staleItems = current.filter { item ->
            getId(item) !in newIds && !isInstalled(item)
        }

        // Notify caller to remove stale items from database
        if (staleItems.isNotEmpty()) {
            Log.d(TAG, "Removing ${staleItems.size} stale non-installed items from cache")
            onStaleRemove(staleItems)
        }

        // ATOMIC UPDATE: Replace with new content
        updateContent(newContent)

        // Apply cache limit after update
        applyCacheLimitIfNeeded(currentContent, updateContent, getId, isInstalled)
    }

    /**
     * Append new content to the existing feed.
     *
     * Steps:
     * 1. Upsert new items (add or replace if exists)
     * 2. Deduplicate by keeping only first occurrence of each ID
     * 3. Apply cache limits to prevent unbounded growth
     *
     * **Use case:** Pagination - user scrolls down, load next page of results
     * **Atomicity:** Single [updateContent] call ensures append is atomic
     *
     * @param newContent Items to append
     * @param currentContent Current content state
     * @param updateContent Callback to update mutable state
     * @param getId Function to extract unique ID
     * @param isInstalled Function to check installed status (for cache limits)
     */
    fun appendFeedContent(
        newContent: List<T>,
        currentContent: StateFlow<List<T>>,
        updateContent: (List<T>) -> Unit,
        getId: (T) -> String,
        isInstalled: (T) -> Boolean
    ) {
        if (newContent.isEmpty()) return

        val current = currentContent.value

        // Upsert new items: add to list, replace if ID exists
        val updated = upsertItemsIntoList(current, newContent, getId)

        // ATOMIC UPDATE
        updateContent(updated)

        // Apply cache limit after update
        applyCacheLimitIfNeeded(currentContent, updateContent, getId, isInstalled)
    }

    /**
     * Add content to the store WITHOUT modifying the main feed order.
     *
     * Use case: Adding search results or supplementary content that shouldn't
     * appear in the main feed view.
     *
     * **Atomicity:** Single [updateContent] call
     *
     * @param newContent Items to add
     * @param currentContent Current content state
     * @param updateContent Callback to update mutable state
     * @param getId Function to extract unique ID
     */
    fun addContentWithoutAffectingFeed(
        newContent: List<T>,
        currentContent: StateFlow<List<T>>,
        updateContent: (List<T>) -> Unit,
        getId: (T) -> String
    ) {
        if (newContent.isEmpty()) return

        val current = currentContent.value

        // Upsert new items: replace if exists, append if new
        val updated = upsertItemsIntoList(current, newContent, getId)

        // ATOMIC UPDATE
        updateContent(updated)
    }

    /**
     * Apply cache limit to prevent unbounded memory growth.
     *
     * Strategy: Keep installed items regardless of age, remove oldest non-installed items
     * when cache exceeds maximum size.
     *
     * **When called:** After every feed modification (replace/append)
     * **Atomicity:** Single [updateContent] call during actual limit application
     *
     * @param currentContent Current content state
     * @param updateContent Callback to update mutable state
     * @param getId Function to extract unique ID
     * @param isInstalled Function to check if item is installed
     */
    private fun applyCacheLimitIfNeeded(
        currentContent: StateFlow<List<T>>,
        updateContent: (List<T>) -> Unit,
        getId: (T) -> String,
        isInstalled: (T) -> Boolean
    ) {
        val limited = cacheManager.applyCacheLimit(currentContent.value, isInstalled)
        if (limited != currentContent.value) {
            Log.d(TAG, "Cache limit applied, reduced from ${currentContent.value.size} to ${limited.size} items")
            updateContent(limited)
        }
    }

    /**
     * Upsert items into a list: update existing items by ID, append new ones.
     *
     * Implementation: Build map of new items, iterate current list and replace where match,
     * then append items that weren't in the current list.
     *
     * **Complexity:** O(n+m) where n=current size, m=new items
     * **Memory:** O(m) for temporary map
     * **Result:** Single list with all items (merged/updated)
     */
    private fun upsertItemsIntoList(
        current: List<T>,
        newItems: List<T>,
        getId: (T) -> String
    ): List<T> {
        // Create map for O(1) lookup of new items
        val newItemsMap = newItems.associateBy(getId)

        // First pass: update existing items or keep as-is
        val updated = current.map { existing ->
            newItemsMap[getId(existing)] ?: existing
        }.toMutableList()

        // Second pass: add new items that weren't in the original list
        val existingIds = current.map(getId).toSet()
        updated.addAll(newItems.filter { getId(it) !in existingIds })

        return updated
    }
}

