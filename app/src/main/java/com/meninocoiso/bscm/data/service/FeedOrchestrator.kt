package com.meninocoiso.bscm.data.service

import android.util.Log
import javax.inject.Inject

private const val TAG = "FeedOrchestrator"
private const val MAX_FEED_CACHE_ITEMS = 50

/**
 * Result of a feed operation containing the updated state.
 * Allows [com.meninocoiso.bscm.data.manager.ContentMemoryStore] to apply state changes and trigger callbacks.
 */
data class FeedUpdateResult<T>(
    val updatedContent: Map<String, T>,
    val updatedFeedOrder: List<String>,
    val staleContent: List<T> = emptyList()
)

/**
 * Pure logic service for managing feed updates with cache limiting.
 * Returns updated state instead of mutating external StateFlows.
 */
class FeedOrchestrator<T> @Inject constructor() {

    /**
     * Computes the result of replacing feed content.
     * Removes stale non-installed items and applies cache limits.
     */
    fun computeReplaceFeed(
        newContent: List<T>,
        currentContent: Map<String, T>,
        // currentFeedOrder unused, removed
        getId: (T) -> String,
        isInstalled: (T) -> Boolean
    ): FeedUpdateResult<T> {
        val newFeedIds = newContent.map { getId(it) }.toSet()

        // Find stale non-installed content
        val contentToRemove = currentContent.values.filter { content ->
            getId(content) !in newFeedIds && !isInstalled(content)
        }

        // Start with current content, remove stale items, then upsert new items
        var updatedMap = currentContent.toMutableMap()
        if (contentToRemove.isNotEmpty()) {
            Log.d(TAG, "Removing ${contentToRemove.size} stale non-installed items from cache")
            contentToRemove.forEach { updatedMap.remove(getId(it)) }
        }

        // Upsert new content
        newContent.forEach { content -> updatedMap[getId(content)] = content }

        // Apply cache limit
        var feedOrder = newContent.map { getId(it) }
        val (limitedOrder, limitedMap) = applyCacheLimit(
            feedOrder,
            updatedMap,
            isInstalled
        )

        return FeedUpdateResult(
            updatedContent = limitedMap,
            updatedFeedOrder = limitedOrder,
            staleContent = contentToRemove
        )
    }

    /**
     * Computes the result of appending content to the feed.
     * Applies cache limits after appending.
     */
    fun computeAppendFeed(
        newContent: List<T>,
        currentContent: Map<String, T>,
        currentFeedOrder: List<String>,
        getId: (T) -> String,
        isInstalled: (T) -> Boolean
    ): FeedUpdateResult<T> {
        if (newContent.isEmpty()) {
            return FeedUpdateResult(currentContent, currentFeedOrder)
        }

        // Upsert new content
        val updatedMap = currentContent.toMutableMap().apply {
            newContent.forEach { content -> put(getId(content), content) }
        }

        // Append to feed order and remove duplicates
        val appendIds = newContent.map { getId(it) }
        val updatedOrder = (currentFeedOrder + appendIds).distinct()

        // Apply cache limit
        val (limitedOrder, limitedMap) = applyCacheLimit(
            updatedOrder,
            updatedMap,
            isInstalled
        )

        return FeedUpdateResult(
            updatedContent = limitedMap,
            updatedFeedOrder = limitedOrder
        )
    }

    /**
     * Computes upsert of content without affecting feed order.
     * Used for search results and other non-feed content.
     */
    fun computeUpsertContent(
        newContent: List<T>,
        currentContent: Map<String, T>,
        getId: (T) -> String
    ): Map<String, T> {
        if (newContent.isEmpty()) return currentContent

        return currentContent.toMutableMap().apply {
            newContent.forEach { content -> put(getId(content), content) }
        }
    }

    /**
     * Apply cache limit to keep memory usage reasonable
     */
    private fun applyCacheLimit(
        feedIds: List<String>,
        contentMap: Map<String, T>,
        isInstalledPredicate: (T) -> Boolean
    ): Pair<List<String>, Map<String, T>> {
        if (contentMap.isEmpty()) {
            return Pair(feedIds, contentMap)
        }

        val nonInstalledFeedIds = feedIds.filter { id ->
            contentMap[id]?.let { !isInstalledPredicate(it) } ?: false
        }

        if (nonInstalledFeedIds.size <= MAX_FEED_CACHE_ITEMS) {
            return Pair(feedIds, contentMap)
        }

        val idsToKeep = nonInstalledFeedIds.take(MAX_FEED_CACHE_ITEMS)
        val keepSet = idsToKeep.toSet()
        val idsToDrop = nonInstalledFeedIds.filterNot { it in keepSet }

        if (idsToDrop.isEmpty()) {
            return Pair(feedIds, contentMap)
        }

        val updatedOrder = feedIds.filterNot { it in idsToDrop }
        val updatedMap = contentMap.toMutableMap().apply {
            idsToDrop.forEach { id ->
                val content = this[id]
                if (content?.let { !isInstalledPredicate(it) } == true) {
                    remove(id)
                }
            }
        }

        Log.d(TAG, "Applied cache limit: keeping ${idsToKeep.size} non-installed items")
        return Pair(updatedOrder, updatedMap)
    }
}