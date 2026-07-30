package com.meninocoiso.bscm.data.mapper

import com.meninocoiso.bscm.data.parser.ContributorParser
import com.meninocoiso.bscm.data.parser.ExternalContentConfig
import com.meninocoiso.bscm.data.parser.ExternalContentMetadata
import com.meninocoiso.bscm.data.service.StreamingLinkParser
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Track
import com.meninocoiso.bscm.domain.model.Version
import java.time.LocalDateTime
import javax.inject.Inject

private const val TAG = "ChartPlaceholderFactory"

/**
 * Factory for creating placeholder Chart objects from external metadata
 */
class ChartPlaceholderFactory @Inject constructor(
    private val difficultyMapper: DifficultyMapper,
    private val streamingLinkParser: StreamingLinkParser,
    private val contributorParser: ContributorParser
) {

    fun createPlaceholderChart(
        metadata: ExternalContentMetadata,
        config: ExternalContentConfig?
    ): Chart {
        val localId = metadata.id.trim()
        val now = LocalDateTime.now()
        return Chart(
            track = Track(
                id = localId,
                title = metadata.title,
                artist = metadata.artist,
                streamingRefs = streamingLinkParser.parseLinks(metadata.streaming),
                duration = 0f,
                coverUrl = metadata.cover,
            ),
            difficulty = difficultyMapper.map(metadata.difficulty),
            notesAmount = metadata.notes ?: 0,
            effectsAmount = metadata.effects ?: 0,
            id = localId,
            downloadsSum = 0,
            updatedAt = metadata.publishedAt?.let {
                LocalDateTime.ofEpochSecond(it, 0, java.time.ZoneOffset.UTC)
            },
            isInstalled = true,
            latestVersion = createPlaceholderVersion(localId, metadata),
            availableVersion = null,
            contributors = contributorParser.parseContributors(metadata.contributors, localId),
            createdAt = now
        )
    }

    private fun createPlaceholderVersion(
        chartId: String,
        metadata: ExternalContentMetadata
    ): Version {
        val now = LocalDateTime.now()
        return Version(
            id = chartId,
            catalogItemId = chartId,
            versionCode = 1,
            downloadsAmount = 0,
            fileSizeBytes = 0L,
            changelog = null,
            discordAttachmentId = null,
            createdAt = metadata.publishedAt?.let {
                LocalDateTime.ofEpochSecond(it, 0, java.time.ZoneOffset.UTC)
            } ?: now,
            bundleHash = null
        )
    }
}
