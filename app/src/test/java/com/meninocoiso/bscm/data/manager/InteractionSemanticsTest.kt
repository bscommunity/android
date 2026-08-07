package com.meninocoiso.bscm.data.manager

import com.meninocoiso.bscm.data.local.entity.QueuedInteractionEntity
import com.meninocoiso.bscm.domain.enums.ActionType
import com.meninocoiso.bscm.domain.enums.CollectionKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InteractionSemanticsTest {

    @Test
    fun `bookmark overlay ignores custom collection mutations`() {
        val pending = listOf(
            queued(id = "chart-1", kind = CollectionKind.USER, action = ActionType.ADD, collectionId = "mixes"),
            queued(id = "chart-2", kind = CollectionKind.USER, action = ActionType.REMOVE, collectionId = "mixes"),
            queued(id = "chart-3", kind = CollectionKind.BOOKMARKS, action = ActionType.ADD),
            queued(id = "chart-4", kind = CollectionKind.BOOKMARKS, action = ActionType.REMOVE),
        )

        val overlay = buildCollectionMembershipOverlay(
            pending = pending,
            collectionKind = CollectionKind.BOOKMARKS,
        )

        assertEquals(setOf("chart-3"), overlay.forceIncludeIds)
        assertEquals(setOf("chart-4"), overlay.forceExcludeIds)
    }

    @Test
    fun `user collection overlay stays scoped to one collection id`() {
        val pending = listOf(
            queued(id = "chart-1", kind = CollectionKind.USER, action = ActionType.ADD, collectionId = "mixes"),
            queued(id = "chart-2", kind = CollectionKind.USER, action = ActionType.ADD, collectionId = "favorites"),
            queued(id = "chart-3", kind = CollectionKind.USER, action = ActionType.REMOVE, collectionId = "mixes"),
        )

        val overlay = buildCollectionMembershipOverlay(
            pending = pending,
            collectionKind = CollectionKind.USER,
            collectionId = "mixes",
        )

        assertEquals(setOf("chart-1"), overlay.forceIncludeIds)
        assertEquals(setOf("chart-3"), overlay.forceExcludeIds)
    }

    @Test
    fun `bookmark state mapping ignores user collection actions`() {
        assertEquals(
            ActionType.ADD,
            bookmarkActionForState(queued(id = "chart-1", kind = CollectionKind.BOOKMARKS, action = ActionType.ADD))
        )
        assertEquals(
            ActionType.REMOVE,
            bookmarkActionForState(queued(id = "chart-1", kind = CollectionKind.BOOKMARKS, action = ActionType.REMOVE))
        )
        assertNull(
            bookmarkActionForState(queued(id = "chart-1", kind = CollectionKind.USER, action = ActionType.ADD, collectionId = "mixes"))
        )
        assertNull(
            bookmarkActionForState(queued(id = "chart-1", kind = CollectionKind.USER, action = ActionType.REMOVE, collectionId = "mixes"))
        )
    }

    private fun queued(
        id: String,
        kind: CollectionKind,
        action: ActionType,
        collectionId: String? = null,
        timestamp: Long = 1L,
    ) = QueuedInteractionEntity(
        id = id,
        collectionId = collectionId,
        collectionKind = kind,
        action = action,
        timestamp = timestamp,
    )
}

