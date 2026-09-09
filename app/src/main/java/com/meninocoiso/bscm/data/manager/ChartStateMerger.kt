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
    private val memoryStore: ContentMemoryStore<Chart>,
) {
    suspend fun mergeRemoteCharts(incoming: List<Chart>): List<Chart> = withContext(Dispatchers.IO) {
        if (incoming.isEmpty()) return@withContext incoming

        val localById = chartDao.getChartsByIds(incoming.map { it.id }).associateBy { it.id }
        val pending = queueManager.getPendingInteractionsSnapshot()
        val latestLikeActions = pending
            .filter { it.collectionKind == CollectionKind.LIKES }
            .associate { it.id to it.action }
        val latestBookmarkActions = pending
            .mapNotNull { interaction ->
                bookmarkActionForState(interaction)?.let { interaction.id to it }
            }
            .toMap()

        incoming.map { remote ->
            val local = localById[remote.id]
            val inMemory = memoryStore.contentById.value[remote.id]

            // Device-only state (install flag and like/bookmark timestamps) is
            // sourced from the live in-memory store, then the persisted Room
            // row, then the server payload. Server payloads never carry these
            // (their defaults are false/null), so a refresh — e.g. the forced
            // refresh after login — must not let them overwrite device state.
            var merged = remote.copy(
                isInstalled = (inMemory?.isInstalled == true) ||
                    (local?.isInstalled == true) ||
                    (remote.isInstalled == true),
                likedAt = inMemory?.likedAt ?: local?.likedAt ?: remote.likedAt,
                bookmarkedAt = inMemory?.bookmarkedAt ?: local?.bookmarkedAt ?: remote.bookmarkedAt,
                availableVersion = remote.availableVersion
                    ?: inMemory?.availableVersion
                    ?: local?.availableVersion,
            )

            val id = merged.id

            when (latestLikeActions[id]) {
                ActionType.ADD -> if (merged.likedAt == null) {
                    merged = merged.copy(
                        likedAt = inMemory?.likedAt ?: local?.likedAt ?: LocalDateTime.now()
                    )
                }
                ActionType.REMOVE -> merged = merged.copy(likedAt = null)
                null -> Unit
            }

            when (latestBookmarkActions[id]) {
                ActionType.ADD -> if (merged.bookmarkedAt == null) {
                    merged = merged.copy(
                        bookmarkedAt = inMemory?.bookmarkedAt ?: local?.bookmarkedAt ?: LocalDateTime.now()
                    )
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