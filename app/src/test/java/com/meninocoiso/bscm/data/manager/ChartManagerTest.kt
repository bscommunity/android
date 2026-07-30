package com.meninocoiso.bscm.data.manager

import com.meninocoiso.bscm.domain.enums.Difficulty
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Contributor
import com.meninocoiso.bscm.domain.model.StreamingRef
import com.meninocoiso.bscm.domain.model.Version
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class ChartManagerTest {

    @Test
    fun `shouldMarkInstalled matches synced charts by contentId only`() {
        val chart = createChart(id = "remote-id", contentId = "content-123")

        val result = ChartManager.shouldMarkInstalled(
            chart = chart,
            installedContentIds = setOf("content-123")
        )

        assertTrue(result)
    }

    @Test
    fun `shouldMarkInstalled does not treat local only charts as canonical installs`() {
        val chart = createChart(id = "local-abc", contentId = null)

        val result = ChartManager.shouldMarkInstalled(
            chart = chart,
            installedContentIds = setOf("local-abc")
        )

        assertFalse(result)
    }

    @Test
    fun `shouldMarkInstalled ignores chart id when contentId is absent from scan`() {
        val chart = createChart(id = "remote-id", contentId = "content-123")

        val result = ChartManager.shouldMarkInstalled(
            chart = chart,
            installedContentIds = setOf("different-content")
        )

        assertFalse(result)
    }

    @Test
    fun `findMissingContentIdsToHydrate returns only unknown canonical contentIds`() {
        val currentCharts = listOf(
            createChart(id = "remote-1", contentId = "content-1"),
            createChart(id = "local-only", contentId = null)
        )

        val result = ChartManager.findMissingContentIdsToHydrate(
            installedContentIds = setOf("content-1", "content-2"),
            currentCharts = currentCharts
        )

        assertTrue("content-2" in result)
        assertFalse("content-1" in result)
    }

    private fun createChart(id: String, contentId: String?): Chart {
        val now = LocalDateTime.of(2026, 3, 6, 10, 0)
        return Chart(
            artist = "Artist",
            track = "Track",
            album = null,
            genre = null,
            colors = emptyList(),
            trackUrls = emptyList<StreamingRef>(),
            id = id,
            contentId = contentId,
            coverUrl = "",
            downloadsSum = 0,
            updatedAt = now,
            contributors = emptyList<Contributor>(),
            createdAt = now,
            isInstalled = false,
            latestVersion = Version(
                id = 1L,
                chartId = id,
                index = 1,
                duration = 0f,
                notesAmount = 0,
                effectsAmount = 0,
                bpm = 0,
                difficulty = Difficulty.NORMAL,
                isDeluxe = false,
                isExplicit = false,
                bundleUrl = "",
                previewUrl = null,
                downloadsAmount = 0,
                knownIssues = emptyList(),
                createdAt = now
            ),
            availableVersion = null
        )
    }
}
