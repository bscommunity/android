package com.meninocoiso.bscm.data.repository

import android.util.Log
import com.meninocoiso.bscm.data.local.dao.ChartDao
import com.meninocoiso.bscm.data.local.dao.CollectionDao
import com.meninocoiso.bscm.data.local.dao.ThemeDao
import com.meninocoiso.bscm.data.local.dao.TourPassDao
import com.meninocoiso.bscm.data.manager.ChartManager
import com.meninocoiso.bscm.data.manager.InteractionQueueManager
import com.meninocoiso.bscm.domain.enums.CollectionKind
import com.meninocoiso.bscm.domain.enums.CatalogItemType
import com.meninocoiso.bscm.domain.enums.OperationOption
import com.meninocoiso.bscm.domain.model.Collection
import com.meninocoiso.bscm.domain.model.CollectionItemCrossRef
import com.meninocoiso.bscm.domain.repository.InteractionRepository
import com.meninocoiso.bscm.domain.result.ContentResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "InteractionRepositoryImpl"
private const val BOOKMARKS_COLLECTION_ID = "bookmarks"
private const val LOCAL_USER_ID = "user"

/**
 * Offline-first implementation for like/bookmark/collection interactions.
 *
 * Local state is updated immediately, then corresponding operations are queued for sync.
 */
@Singleton
class InteractionRepositoryImpl @Inject constructor(
    private val queueManager: InteractionQueueManager,
    private val chartManager: ChartManager,
    private val chartDao: ChartDao,
    private val tourPassDao: TourPassDao,
    private val themeDao: ThemeDao,
    private val collectionDao: CollectionDao,
    private val profileCacheRepository: ProfileCacheRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : InteractionRepository {

    private data class SavedSnapshot(
        val hasBookmarkMembership: Boolean,
        val userCollectionIds: List<String>,
        val isSaved: Boolean,
    )

    /**
     * Marks content as liked locally and queues remote sync.
     */
    override suspend fun likeContent(id: String): Result<Unit> =
        withContext(dispatcher) {
            runCatching {
                val contentType = resolveContentType(id)
                val shouldIncrement = when (contentType) {
                    CatalogItemType.TOUR_PASS -> tourPassDao.getTourPass(id)?.likedAt == null
                    CatalogItemType.THEME -> themeDao.getTheme(id)?.likedAt == null
                    else -> chartDao.getChart(id)?.likedAt == null
                }
                updateLocalState(id = id, operation = OperationOption.LIKE)
                if (shouldIncrement) {
                    profileCacheRepository.adjustLikesCounts(contentType, delta = 1)
                }
                queueManager.queueAndSyncLike(id, isLike = true)
            }.onFailure { e ->
                Log.e(TAG, "Unexpected error in likeContent for id: $id", e)
            }
        }

    /**
     * Removes local like state and queues remote sync.
     */
    override suspend fun unlikeContent(id: String): Result<Unit> =
        withContext(dispatcher) {
            runCatching {
                val contentType = resolveContentType(id)
                val shouldDecrement = when (contentType) {
                    CatalogItemType.TOUR_PASS -> tourPassDao.getTourPass(id)?.likedAt != null
                    CatalogItemType.THEME -> themeDao.getTheme(id)?.likedAt != null
                    else -> chartDao.getChart(id)?.likedAt != null
                }
                updateLocalState(id = id, operation = OperationOption.UNLIKE)
                if (shouldDecrement) {
                    profileCacheRepository.adjustLikesCounts(contentType, delta = -1)
                }
                queueManager.queueAndSyncLike(id, isLike = false)
            }.onFailure { e ->
                Log.e(TAG, "Unexpected error in unlikeContent for id: $id", e)
            }
        }

    /**
     * Ensures bookmark membership exists locally and queues bookmark sync.
     */
    override suspend fun bookmarkContent(id: String): Result<Unit> =
        withContext(dispatcher) {
            runCatching {
                val before = getSavedSnapshot(id)

                ensureBookmarksCollectionExists()
                if (!before.hasBookmarkMembership) {
                    collectionDao.upsertCrossRef(
                        CollectionItemCrossRef(
                            collectionId = BOOKMARKS_COLLECTION_ID,
                            id = id,
                            contentType = resolveContentType(id)
                        )
                    )
                }
                syncSavedState(id, wasSaved = before.isSaved, isSaved = true)
                queueManager.queueAndSyncBookmark(id, isBookmarked = true)
            }.onFailure { e ->
                Log.e(TAG, "Unexpected error in bookmarkContent for id: $id", e)
            }
        }

    /**
     * Removes only auto-bookmarks membership and queues unbookmark sync.
     *
     * Custom USER collection memberships are intentionally preserved by contract.
     */
    override suspend fun unbookmarkContent(id: String): Result<Unit> =
        withContext(dispatcher) {
            runCatching {
                val before = getSavedSnapshot(id)

                collectionDao.deleteCrossRef(BOOKMARKS_COLLECTION_ID, id)

                // New contract: unbookmark only removes auto-bookmarks membership.
                // Saved status now depends on whether custom memberships still exist.
                val isStillSaved = before.userCollectionIds.isNotEmpty()
                syncSavedState(id, wasSaved = before.isSaved, isSaved = isStillSaved)

                queueManager.queueAndSyncBookmark(id, isBookmarked = false)
            }.onFailure { e ->
                Log.e(TAG, "Unexpected error in unbookmarkContent for id: $id", e)
            }
        }

    /**
     * Adds content to a custom collection and guarantees bookmark baseline semantics.
     */
    override suspend fun addToCollection(
        id: String,
        collectionId: String
    ): Result<Unit> =
        withContext(dispatcher) {
            runCatching {
                val before = getSavedSnapshot(id)
                val hasCollectionMembership = collectionDao.hasCrossRef(collectionId, id)
                val now = LocalDateTime.now()

                ensureBookmarksCollectionExists(now)
                val contentType = resolveContentType(id)
                if (!before.hasBookmarkMembership) {
                    collectionDao.upsertCrossRef(
                        CollectionItemCrossRef(
                            collectionId = BOOKMARKS_COLLECTION_ID,
                            id = id,
                            contentType = contentType,
                            addedAt = now,
                        )
                    )
                }
                if (!hasCollectionMembership) {
                    collectionDao.upsertCrossRef(
                        CollectionItemCrossRef(
                            collectionId = collectionId,
                            id = id,
                            contentType = contentType,
                            addedAt = now,
                        )
                    )
                    // The just-added item is now the collection's most recent one,
                    // so its cover becomes the collection cover — mirroring the
                    // server's "latest added item" rule. Without this the local
                    // cache (and the UI) keeps the null cover written at creation
                    // time until the next server fetch resolves it.
                    updateCollectionCoverIfNeeded(collectionId, id, contentType)
                }
                // Any custom collection membership implies content is considered saved.
                syncSavedState(id, wasSaved = before.isSaved, isSaved = true)

                if (!before.hasBookmarkMembership) {
                    queueManager.queueAndSyncBookmark(id, isBookmarked = true)
                }
                queueManager.queueAndSyncCollection(id, collectionId, isAdd = true)
            }.onFailure { e ->
                Log.e(TAG, "Failed to add to collection for id: $id, collectionId: $collectionId", e)
            }
        }

    /**
     * Removes membership from one collection and reconciles local saved state.
     */
    override suspend fun removeFromCollection(
        id: String,
        collectionId: String
    ): Result<Unit> =
        withContext(dispatcher) {
            runCatching {
                val before = getSavedSnapshot(id)
                val hadCollectionMembership = collectionDao.hasCrossRef(collectionId, id)

                if (hadCollectionMembership) {
                    collectionDao.deleteCrossRef(collectionId, id)
                }

                // Recompute saved status after local delete to keep counters/chart flags consistent.
                val hasBookmarkMembershipAfter =
                    if (collectionId == BOOKMARKS_COLLECTION_ID) false
                    else collectionDao.hasCrossRef(BOOKMARKS_COLLECTION_ID, id)
                val userCollectionIdsAfter = collectionDao.getUserCollectionIdsForContent(id)
                val isSavedAfter = hasBookmarkMembershipAfter || userCollectionIdsAfter.isNotEmpty()
                syncSavedState(id, wasSaved = before.isSaved, isSaved = isSavedAfter)

                if (collectionId == BOOKMARKS_COLLECTION_ID) {
                    queueManager.queueAndSyncBookmark(id, isBookmarked = false)
                } else if (hadCollectionMembership) {
                    queueManager.queueAndSyncCollection(id, collectionId, isAdd = false)
                }
            }.onFailure { e ->
                Log.e(TAG, "Failed to remove from collection for id: $id, collectionId: $collectionId", e)
            }
        }


    /** Returns pending interaction queue size. */
    override suspend fun getQueueSize(): Int = queueManager.getQueueSize()

    /** Triggers immediate processing of queued interactions. */
    override suspend fun processQueue() = queueManager.processQueuedInteractions()

    /**
     * Creates/updates the local synthetic Bookmarks collection used by offline membership logic.
     */
    private suspend fun ensureBookmarksCollectionExists(updatedAt: LocalDateTime = LocalDateTime.now()) {
        collectionDao.upsertCollection(
            Collection(
                id = BOOKMARKS_COLLECTION_ID,
                userId = LOCAL_USER_ID,
                kind = CollectionKind.BOOKMARKS,
                name = "Bookmarks",
                isPublic = false,
                createdAt = updatedAt,
                updatedAt = updatedAt,
            )
        )
    }

    /**
     * Captures local membership and item flags used to detect saved-state transitions.
     */
    private suspend fun getSavedSnapshot(id: String): SavedSnapshot {
        val itemBookmarkedAt = when (resolveContentType(id)) {
            CatalogItemType.TOUR_PASS -> tourPassDao.getTourPass(id)?.bookmarkedAt
            CatalogItemType.THEME -> themeDao.getTheme(id)?.bookmarkedAt
            else -> chartDao.getChart(id)?.bookmarkedAt
        }
        val hasBookmarkMembership = collectionDao.hasCrossRef(BOOKMARKS_COLLECTION_ID, id)
        val userCollectionIds = collectionDao.getUserCollectionIdsForContent(id)
        return SavedSnapshot(
            hasBookmarkMembership = hasBookmarkMembership,
            userCollectionIds = userCollectionIds,
            isSaved = itemBookmarkedAt != null || hasBookmarkMembership || userCollectionIds.isNotEmpty(),
        )
    }

    /**
     * Applies bookmark/unbookmark side effects only when saved state actually transitions.
     */
    private suspend fun syncSavedState(id: String, wasSaved: Boolean, isSaved: Boolean) {
        when {
            !wasSaved && isSaved -> {
                updateLocalState(id = id, operation = OperationOption.BOOKMARK)
                profileCacheRepository.adjustBookmarksCounts(resolveContentType(id), delta = 1)
            }

            wasSaved && !isSaved -> {
                updateLocalState(id = id, operation = OperationOption.UNBOOKMARK)
                profileCacheRepository.adjustBookmarksCounts(resolveContentType(id), delta = -1)
            }
        }
    }

    /**
     * Updates local state for the content identified by `id` (the primary key).
     * Used by like/unlike/bookmark/unbookmark where we always have the item id.
     */
    private suspend fun updateLocalState(id: String, operation: OperationOption) {
        when (resolveContentType(id)) {
            CatalogItemType.TOUR_PASS -> {
                when (operation) {
                    OperationOption.LIKE -> tourPassDao.updateLikedAt(id, epochMillisNow())
                    OperationOption.UNLIKE -> tourPassDao.updateLikedAt(id, null)
                    OperationOption.BOOKMARK -> tourPassDao.updateBookmarkedAt(id, epochMillisNow())
                    OperationOption.UNBOOKMARK -> tourPassDao.updateBookmarkedAt(id, null)
                    else -> {}
                }
            }

            CatalogItemType.THEME -> {
                when (operation) {
                    OperationOption.LIKE -> themeDao.updateLikedAt(id, epochMillisNow())
                    OperationOption.UNLIKE -> themeDao.updateLikedAt(id, null)
                    OperationOption.BOOKMARK -> themeDao.updateBookmarkedAt(id, epochMillisNow())
                    OperationOption.UNBOOKMARK -> themeDao.updateBookmarkedAt(id, null)
                    else -> {}
                }
            }

            else -> {
                try {
                    val result = chartManager.updateContentById(id, operation)
                    if (result !is ContentResult.Success) {
                        Log.e(TAG, "Failed to update local chart for id=$id, operation=$operation, result=$result")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception updating local chart for id=$id, operation=$operation", e)
                }
            }
        }
    }

    /**
     * Resolves the type of an item by probing the local tables.
     * Defaults to [CatalogItemType.CHART] to keep the legacy chart-first behavior.
     */
    private fun resolveContentType(id: String): CatalogItemType = when {
        tourPassDao.getTourPass(id) != null -> CatalogItemType.TOUR_PASS
        themeDao.getTheme(id) != null -> CatalogItemType.THEME
        else -> CatalogItemType.CHART
    }

    /**
     * Writes the item's cover onto the collection row when the item was newly
     * added. The Bookmarks collection is skipped: it is a local synthetic
     * collection that the server never gives a cover.
     */
    private suspend fun updateCollectionCoverIfNeeded(
        collectionId: String,
        id: String,
        contentType: CatalogItemType,
    ) {
        if (collectionId == BOOKMARKS_COLLECTION_ID) return
        val coverUrl = when (contentType) {
            CatalogItemType.TOUR_PASS -> tourPassDao.getTourPass(id)?.coverUrl
            CatalogItemType.THEME -> themeDao.getTheme(id)?.coverUrl
            else -> chartDao.getChart(id)?.track?.coverUrl
        }
        if (!coverUrl.isNullOrBlank()) {
            collectionDao.updateCollectionCoverUrl(collectionId, coverUrl)
        }
    }

    private fun epochMillisNow(): Long =
        LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}