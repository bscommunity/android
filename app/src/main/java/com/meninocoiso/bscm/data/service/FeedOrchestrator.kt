package com.meninocoiso.bscm.data.service

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

private const val TAG = "FeedOrchestrator"

/**
 * Generic orchestrator for managing feed updates and cache
 */
class FeedOrchestrator<T> @Inject constructor(
    private val cacheManager: ContentCacheManager
) {

    fun replaceFeedContent(
        newContent: List<T>,
        contentMap: StateFlow<Map<String, T>>,
        feedOrder: StateFlow<List<String>>,
        updateContentMap: (Map<String, T>) -> Unit,
        updateFeedOrder: (List<String>) -> Unit,
        getId: (T) -> String,
        isInstalled: (T) -> Boolean,
        onStaleRemove: (List<T>) -> Unit = {},
        coroutineScope: CoroutineScope
    ) {
        val newFeedIds = newContent.map { getId(it) }.toSet()
        val currentChartMap = contentMap.value

        // Find content that is in memory but not in the new feed and not installed
        val contentToRemove = currentChartMap.values.filter { content ->
            getId(content) !in newFeedIds && !isInstalled(content)
        }

        // Remove stale content from database
        if (contentToRemove.isNotEmpty()) {
            Log.d(TAG, "Removing ${contentToRemove.size} stale non-installed items from cache")
            onStaleRemove(contentToRemove)

            // Remove from memory immediately
            val updatedMap = currentChartMap.toMutableMap().apply {
                contentToRemove.forEach { remove(getId(it)) }
            }
            updateContentMap(updatedMap)
        }

        // Upsert new content and update feed order
        upsertContent(newContent, contentMap, updateContentMap, getId)
        updateFeedOrder(newContent.map { getId(it) })
        
        // Apply cache limit
        val (limitedOrder, limitedMap) = cacheManager.applyCacheLimit(
            feedOrder.value,
            contentMap.value,
            isInstalled
        )
        updateFeedOrder(limitedOrder)
        updateContentMap(limitedMap)
    }

    fun appendFeedContent(
        newContent: List<T>,
        contentMap: StateFlow<Map<String, T>>,
        feedOrder: StateFlow<List<String>>,
        updateContentMap: (Map<String, T>) -> Unit,
        updateFeedOrder: (List<String>) -> Unit,
        getId: (T) -> String,
        isInstalled: (T) -> Boolean
    ) {
        if (newContent.isEmpty()) return

        upsertContent(newContent, contentMap, updateContentMap, getId)
        val appendIds = newContent.map { getId(it) }
        if (appendIds.isNotEmpty()) {
            val updated = (feedOrder.value + appendIds).distinct()
            updateFeedOrder(updated)
            
            // Apply cache limit
            val (limitedOrder, limitedMap) = cacheManager.applyCacheLimit(
                updated,
                contentMap.value,
                isInstalled
            )
            updateFeedOrder(limitedOrder)
            updateContentMap(limitedMap)
        }
    }

    fun addContentWithoutAffectingFeed(
        newContent: List<T>,
        contentMap: StateFlow<Map<String, T>>,
        updateContentMap: (Map<String, T>) -> Unit,
        getId: (T) -> String
    ) {
        upsertContent(newContent, contentMap, updateContentMap, getId)
    }

    private fun upsertContent(
        newContent: List<T>,
        contentMap: StateFlow<Map<String, T>>,
        updateContentMap: (Map<String, T>) -> Unit,
        getId: (T) -> String
    ) {
        if (newContent.isEmpty()) return

        val updated = contentMap.value.toMutableMap().apply {
            newContent.forEach { content ->
                put(getId(content), content)
            }
        }
        updateContentMap(updated)
    }
}

