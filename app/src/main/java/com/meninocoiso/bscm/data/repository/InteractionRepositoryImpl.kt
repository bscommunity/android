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
    override suspend fun likeContent(id: String, contentId: String): Result<Unit> =
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
                queueManager.queueAndSyncLike(contentId, isLike = true)
            }.onFailure { e ->
                Log.e(TAG, "Unexpected error in likeContent for contentId: $contentId", e)
            }
        }

    /**
     * Removes local like state and queues remote sync.
     */
    override suspend fun unlikeContent(id: String, contentId: String): Result<Unit> =
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
                queueManager.queueAndSyncLike(contentId, isLike = false)
            }.onFailure { e ->
                Log.e(TAG, "Unexpected error in unlikeContent for contentId: $contentId", e)
            }
        }

    /**
     * Ensures bookmark membership exists locally and queues bookmark sync.
     */
    override suspend fun bookmarkContent(id: String, contentId: String): Result<Unit> =
        withContext(dispatcher) {
            runCatching {
                val before = getSavedSnapshot(id, contentId)

                ensureBookmarksCollectionExists()
                if (!before.hasBookmarkMembership) {
                    collectionDao.upsertCrossRef(
                        CollectionItemCrossRef(
                            collectionId = BOOKMARKS_COLLECTION_ID,
                            contentId = contentId,
                            contentType = resolveContentType(contentId)
                        )
                    )
                }
                syncSavedState(id, wasSaved = before.isSaved, isSaved = true)
                queueManager.queueAndSyncBookmark(contentId, isBookmarked = true)
            }.onFailure { e ->
                Log.e(TAG, "Unexpected error in bookmarkContent for contentId: $contentId", e)
            }
        }

    /**
     * Removes only auto-bookmarks membership and queues unbookmark sync.
     *
     * Custom USER collection memberships are intentionally preserved by contract.
     */
    override suspend fun unbookmarkContent(id: String, contentId: String): Result<Unit> =
        withContext(dispatcher) {
            runCatching {
                val before = getSavedSnapshot(id, contentId)

                collectionDao.deleteCrossRef(BOOKMARKS_COLLECTION_ID, contentId)

                // New contract: unbookmark only removes auto-bookmarks membership.
                // Saved status now depends on whether custom memberships still exist.
                val isStillSaved = before.userCollectionIds.isNotEmpty()
                syncSavedState(id, wasSaved = before.isSaved, isSaved = isStillSaved)

                queueManager.queueAndSyncBookmark(contentId, isBookmarked = false)
            }.onFailure { e ->
                Log.e(TAG, "Unexpected error in unbookmarkContent for contentId: $contentId", e)
            }
        }

    /**
     * Adds content to a custom collection and guarantees bookmark baseline semantics.
     */
    override suspend fun addToCollection(
        id: String,
        contentId: String,
        collectionId: String
    ): Result<Unit> =
        withContext(dispatcher) {
            runCatching {
                val before = getSavedSnapshot(id, contentId)
                val hasCollectionMembership = collectionDao.hasCrossRef(collectionId, contentId)
                val now = LocalDateTime.now()

                ensureBookmarksCollectionExists(now)
                val contentType = resolveContentType(contentId)
                if (!before.hasBookmarkMembership) {
                    collectionDao.upsertCrossRef(
                        CollectionItemCrossRef(
                            collectionId = BOOKMARKS_COLLECTION_ID,
                            contentId = contentId,
                            contentType = contentType,
                            addedAt = now,
                        )
                    )
                }
                if (!hasCollectionMembership) {
                    collectionDao.upsertCrossRef(
                        CollectionItemCrossRef(
                            collectionId = collectionId,
                            contentId = contentId,
                            contentType = contentType,
                            addedAt = now,
                        )
                    )
                }
                // Any custom collection membership implies content is considered saved.
                syncSavedState(id, wasSaved = before.isSaved, isSaved = true)

                if (!before.hasBookmarkMembership) {
                    queueManager.queueAndSyncBookmark(contentId, isBookmarked = true)
                }
                queueManager.queueAndSyncCollection(contentId, collectionId, isAdd = true)
            }.onFailure { e ->
                Log.e(TAG, "Failed to add to collection for contentId: $contentId, collectionId: $collectionId", e)
            }
        }

    /**
     * Removes membership from one collection and reconciles local saved state.
     */
    override suspend fun removeFromCollection(
        id: String,
        contentId: String,
        collectionId: String
    ): Result<Unit> =
        withContext(dispatcher) {
            runCatching {
                val before = getSavedSnapshot(id, contentId)
                val hadCollectionMembership = collectionDao.hasCrossRef(collectionId, contentId)

                if (hadCollectionMembership) {
                    collectionDao.deleteCrossRef(collectionId, contentId)
                }

                // Recompute saved status after local delete to keep counters/chart flags consistent.
                val hasBookmarkMembershipAfter =
                    if (collectionId == BOOKMARKS_COLLECTION_ID) false
                    else collectionDao.hasCrossRef(BOOKMARKS_COLLECTION_ID, contentId)
                val userCollectionIdsAfter = collectionDao.getUserCollectionIdsForContent(contentId)
                val isSavedAfter = hasBookmarkMembershipAfter || userCollectionIdsAfter.isNotEmpty()
                syncSavedState(id, wasSaved = before.isSaved, isSaved = isSavedAfter)

                if (collectionId == BOOKMARKS_COLLECTION_ID) {
                    queueManager.queueAndSyncBookmark(contentId, isBookmarked = false)
                } else if (hadCollectionMembership) {
                    queueManager.queueAndSyncCollection(contentId, collectionId, isAdd = false)
                }
            }.onFailure { e ->
                Log.e(TAG, "Failed to remove from collection for contentId: $contentId, collectionId: $collectionId", e)
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
    private suspend fun getSavedSnapshot(id: String, contentId: String): SavedSnapshot {
        val itemBookmarkedAt = when (resolveContentType(id)) {
            CatalogItemType.TOUR_PASS -> tourPassDao.getTourPass(id)?.bookmarkedAt
            CatalogItemType.THEME -> themeDao.getTheme(id)?.bookmarkedAt
            else -> chartDao.getChart(id)?.bookmarkedAt
        }
        val hasBookmarkMembership = collectionDao.hasCrossRef(BOOKMARKS_COLLECTION_ID, contentId)
        val userCollectionIds = collectionDao.getUserCollectionIdsForContent(contentId)
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
     * Resolves the catalog type of an item by probing the local tables.
     * Defaults to [CatalogItemType.CHART] to keep the legacy chart-first behavior.
     */
    private fun resolveContentType(id: String): CatalogItemType = when {
        tourPassDao.getTourPass(id) != null -> CatalogItemType.TOUR_PASS
        themeDao.getTheme(id) != null -> CatalogItemType.THEME
        else -> CatalogItemType.CHART
    }

    private fun epochMillisNow(): Long =
        LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
}