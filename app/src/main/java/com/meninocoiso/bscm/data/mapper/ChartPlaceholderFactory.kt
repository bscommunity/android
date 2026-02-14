package com.meninocoiso.bscm.data.mapper

import com.meninocoiso.bscm.data.parser.ContributorParser
import com.meninocoiso.bscm.data.parser.ExternalContentConfig
import com.meninocoiso.bscm.data.parser.ExternalContentMetadata
import com.meninocoiso.bscm.data.service.StreamingLinkParser
import com.meninocoiso.bscm.domain.model.Chart
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
        config: ExternalContentConfig
    ): Chart {
        val now = LocalDateTime.now()
        return Chart(
            artist = metadata.artist,
            track = metadata.title,
            album = null,
            genre = null,
            colors = config.songTemplate.colorGradient.map { it.color },
            trackUrls = streamingLinkParser.parseLinks(metadata.streaming),
            id = metadata.id,
            contentId = metadata.contentId,
            coverUrl = metadata.cover ?: "",
            downloadsSum = 0,
            updatedAt = metadata.publishedAt?.let {
                LocalDateTime.ofEpochSecond(it, 0, java.time.ZoneOffset.UTC)
            } ?: now,
            isInstalled = true,
            latestVersion = createPlaceholderVersion(metadata.id, metadata),
            availableVersion = null,
            contributors = contributorParser.parseContributors(metadata.contributors, metadata.id),
            createdAt = now
        )
    }

    private fun createPlaceholderVersion(
        chartId: String,
        metadata: ExternalContentMetadata
    ): Version {
        val now = LocalDateTime.now()
        return Version(
            id = -chartId.hashCode().toLong(),
            chartId = chartId,
            index = 1,
            duration = metadata.duration ?: 0f,
            notesAmount = metadata.notes ?: 0,
            effectsAmount = metadata.effects ?: 0,
            bpm = metadata.bpm?.toInt() ?: 0,
            difficulty = difficultyMapper.map(metadata.difficulty),
            isDeluxe = metadata.type.equals("Promode", ignoreCase = true),
            isExplicit = false,
            bundleUrl = "",
            previewUrl = metadata.gameplay,
            downloadsAmount = 0,
            knownIssues = emptyList(),
            createdAt = metadata.publishedAt?.let {
                LocalDateTime.ofEpochSecond(it, 0, java.time.ZoneOffset.UTC)
            } ?: now
        )
    }
}

