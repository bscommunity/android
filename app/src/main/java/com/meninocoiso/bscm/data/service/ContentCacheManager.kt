package com.meninocoiso.bscm.data.service

import android.util.Log
import javax.inject.Inject

private const val TAG = "ContentCacheManager"
private const val MAX_CACHED_CONTENT = 50

/**
 * Generic cache manager for limiting stored content
 */
class ContentCacheManager @Inject constructor() {

    /**
     * Apply cache limit to keep memory usage reasonable
     * @param feedIds Current feed order
     * @param contentMap Current content map
     * @param isInstalledPredicate Function to check if content is installed
     * @return Pair of (updatedFeedIds, updatedContentMap)
     */
    fun <T> applyCacheLimit(
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

        if (nonInstalledFeedIds.size <= MAX_CACHED_CONTENT) {
            return Pair(feedIds, contentMap)
        }

        val idsToKeep = nonInstalledFeedIds.take(MAX_CACHED_CONTENT)
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

