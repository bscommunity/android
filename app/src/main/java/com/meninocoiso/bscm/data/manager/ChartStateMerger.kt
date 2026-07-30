package com.meninocoiso.bscm.data.manager

import com.meninocoiso.bscm.data.local.dao.ChartDao
import com.meninocoiso.bscm.data.local.entity.QueuedInteractionEntity
import com.meninocoiso.bscm.domain.enums.ActionType
import com.meninocoiso.bscm.domain.enums.CollectionKind
import com.meninocoiso.bscm.domain.model.Chart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

internal fun bookmarkActionForState(interaction: QueuedInteractionEntity): ActionType? =
    if (interaction.collectionKind == CollectionKind.BOOKMARKS) interaction.action else null

@Singleton
class ChartStateMerger @Inject constructor(
    private val chartDao: ChartDao,
    private val queueManager: InteractionQueueManager,
) {
    suspend fun mergeRemoteCharts(incoming: List<Chart>): List<Chart> = withContext(Dispatchers.IO) {
        if (incoming.isEmpty()) return@withContext incoming

        val localById = chartDao.getChartsByIds(incoming.map { it.id }).associateBy { it.id }
        val pending = queueManager.getPendingInteractionsSnapshot()
        val latestLikeActions = pending
            .filter { it.collectionKind == CollectionKind.LIKES }
            .associate { it.contentId to it.action }
        val latestBookmarkActions = pending
            .mapNotNull { interaction ->
                bookmarkActionForState(interaction)?.let { interaction.contentId to it }
            }
            .toMap()

        incoming.map { remote ->
            val local = localById[remote.id]
            var merged = if (local != null) {
                remote.copy(
                    isInstalled = (local.isInstalled == true) || (remote.isInstalled == true),
                    likedAt = local.likedAt ?: remote.likedAt,
                    bookmarkedAt = local.bookmarkedAt ?: remote.bookmarkedAt,
                    availableVersion = remote.availableVersion ?: local.availableVersion,
                )
            } else {
                remote
            }

            val contentId = merged.id

            when (latestLikeActions[contentId]) {
                ActionType.ADD -> if (merged.likedAt == null) {
                    merged = merged.copy(likedAt = local?.likedAt ?: LocalDateTime.now())
                }
                ActionType.REMOVE -> merged = merged.copy(likedAt = null)
                null -> Unit
            }

            when (latestBookmarkActions[contentId]) {
                ActionType.ADD -> if (merged.bookmarkedAt == null) {
                    merged = merged.copy(bookmarkedAt = local?.bookmarkedAt ?: LocalDateTime.now())
                }
                ActionType.REMOVE -> merged = merged.copy(bookmarkedAt = null)
                null -> Unit
            }

            merged
        }
    }

    suspend fun getChartsByIds(ids: Collection<String>): List<Chart> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext emptyList()
        chartDao.getChartsByIds(ids.toList())
    }
}