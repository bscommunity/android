package com.meninocoiso.bscm.data.manager

import com.meninocoiso.bscm.domain.enums.Difficulty
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Track
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ChartManagerTest {

    @Test
    fun `shouldMarkInstalled matches synced charts by id`() {
        val chart = createChart(id = "content-123")

        val result = ChartManager.shouldMarkInstalled(
            chart = chart,
            installedIds = setOf("content-123")
        )

        assertTrue(result)
    }

    @Test
    fun `shouldMarkInstalled ignores chart when its id is absent from scan`() {
        val chart = createChart(id = "content-123")

        val result = ChartManager.shouldMarkInstalled(
            chart = chart,
            installedIds = setOf("different-content")
        )

        assertFalse(result)
    }

    @Test
    fun `findMissingIdsToHydrate returns only unknown ids`() {
        val currentCharts = listOf(
            createChart(id = "content-1"),
            createChart(id = "local-only")
        )

        val result = ChartManager.findMissingIdsToHydrate(
            installedIds = setOf("content-1", "content-2"),
            currentCharts = currentCharts
        )

        assertTrue("content-2" in result)
        assertFalse("content-1" in result)
    }

    private fun createChart(id: String): Chart {
        val now = LocalDateTime.of(2026, 3, 6, 10, 0)
        return Chart(
            track = Track(
                id = "track-$id",
                title = "Track",
                artist = "Artist",
                duration = 120f,
            ),
            difficulty = Difficulty.NORMAL,
            notesAmount = 100,
            effectsAmount = 10,
            id = id,
            downloadsSum = 0,
            createdAt = now,
            updatedAt = now,
            contributors = emptyList(),
            isInstalled = false,
            latestVersion = null,
            availableVersion = null,
        )
    }
}
