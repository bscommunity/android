package com.meninocoiso.bscm.data.service

import android.util.Log
import javax.inject.Inject

private const val TAG = "ContentCacheManager"
private const val MAX_CACHED_CONTENT = 50

/**
 * Manages cache limits for content storage.
 *
 * **Strategy:** Keep all installed items, remove oldest non-installed items when cache exceeds limit.
 *
 * Rationale:
 * - Installed items must be kept (user has explicitly installed them)
 * - Non-installed items are cached for quick access but can be pruned
 * - Keep newest non-installed items (most likely to be re-accessed)
 * - Remove oldest non-installed items first (least likely to be needed)
 *
 * **Atomicity:** Works with complete lists to avoid partial state.
 */
class ContentCacheManager @Inject constructor() {

    /**
     * Apply cache limit to prevent unbounded memory growth.
     *
     * **Algorithm:**
     * 1. Count non-installed items (these are the only ones we can remove)
     * 2. If under limit, return unchanged
     * 3. If over limit, keep newest N items, remove oldest M items
     * 4. Preserve all installed items regardless of count
     *
     * **Complexity:** O(n) where n = list size (single pass)
     * **Memory:** O(k) where k = items to remove
     * **Result:** List with cache limit applied, preserving order
     *
     * @param items Current items in feed order
     * @param isInstalledPredicate Function to check if item is installed
     * @return List with cache limit applied (same order, potentially fewer items)
     */
    fun <T> applyCacheLimit(
        items: List<T>,
        isInstalledPredicate: (T) -> Boolean
    ): List<T> {
        if (items.isEmpty()) return items

        // Partition items into installed and non-installed, preserving order
        val installed = mutableListOf<T>()
        val nonInstalled = mutableListOf<T>()

        for (item in items) {
            if (isInstalledPredicate(item)) {
                installed.add(item)
            } else {
                nonInstalled.add(item)
            }
        }

        // If non-installed items are within limit, keep everything
        if (nonInstalled.size <= MAX_CACHED_CONTENT) {
            return items
        }

        // Over limit: keep newest non-installed items, remove oldest
        val nonInstalledToKeep = nonInstalled.takeLast(MAX_CACHED_CONTENT)

        // Rebuild list with all installed items and kept non-installed items, preserving original order
        val result = items.filter { item ->
            isInstalledPredicate(item) || item in nonInstalledToKeep
        }

        val removed = items.size - result.size
        if (removed > 0) {
            Log.d(TAG, "Applied cache limit: removed $removed items, keeping ${result.size}")
        }

        return result
    }
}

