package com.meninocoiso.bscm.domain.serialization

import androidx.room.TypeConverter
import com.meninocoiso.bscm.domain.enums.ContentType
import com.meninocoiso.bscm.domain.enums.Difficulty
import com.meninocoiso.bscm.domain.model.Contributor
import com.meninocoiso.bscm.domain.model.InteractionType
import com.meninocoiso.bscm.domain.model.KnownIssue
import com.meninocoiso.bscm.domain.model.StreamingLink
import com.meninocoiso.bscm.domain.model.Version
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDateTime? {
        return value?.let {
            Instant.ofEpochMilli(it)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
        }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDateTime?): Long? {
        return date?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    }

    // Chart
    private val json = Json { ignoreUnknownKeys = true }

    // StreamingLink
    @TypeConverter
    fun fromStreamingLinkList(streamingLinks: List<StreamingLink>): String {
        return json.encodeToString(streamingLinks)
    }

    @TypeConverter
    fun toStreamingLinkList(streamingLinksString: String): List<StreamingLink> {
        return if (streamingLinksString.isBlank()) {
            emptyList()
        } else {
            json.decodeFromString(streamingLinksString)
        }
    }

    // Contributor List converters
    @TypeConverter
    fun fromContributorsList(contributors: List<Contributor>): String {
        return json.encodeToString(contributors)
    }

    @TypeConverter
    fun toContributorsList(contributorsString: String): List<Contributor> {
        return if (contributorsString.isBlank()) {
            emptyList()
        } else {
            json.decodeFromString(contributorsString)
        }
    }

    // DifficultyEnum converters
    @TypeConverter
    fun fromDifficultyEnum(difficulty: Difficulty): String {
        return difficulty.name
    }

    @TypeConverter
    fun toDifficultyEnum(difficultyString: String): Difficulty {
        return try {
            Difficulty.valueOf(difficultyString)
        } catch (e: IllegalArgumentException) {
            Difficulty.NORMAL // Default value if conversion fails
        }
    }

    // Version
    @TypeConverter
    fun fromKnownIssuesList(knownIssues: List<KnownIssue>): String {
        return json.encodeToString(knownIssues)
    }

    @TypeConverter
    fun toKnownIssuesList(knownIssuesString: String): List<KnownIssue> {
        return if (knownIssuesString.isBlank()) {
            emptyList()
        } else {
            json.decodeFromString(knownIssuesString)
        }
    }

    @TypeConverter
    fun fromVersion(version: Version?): String? {
        return version?.let { json.encodeToString(it) }
    }

    @TypeConverter
    fun toVersion(versionString: String?): Version? {
        return versionString?.let {
            if (it.isBlank()) null
            else json.decodeFromString(versionString)
        }
    }

    @TypeConverter
    fun fromStringList(stringList: List<String>): String {
        return json.encodeToString(stringList)
    }

    @TypeConverter
    fun toStringList(stringListString: String): List<String> {
        return if (stringListString.isBlank()) {
            emptyList()
        } else {
            json.decodeFromString(stringListString)
        }
    }

    // InteractionType converters
    @TypeConverter
    fun fromInteractionType(interactionType: InteractionType): String {
        return interactionType.name
    }

    @TypeConverter
    fun toInteractionType(interactionTypeString: String): InteractionType {
        return try {
            InteractionType.valueOf(interactionTypeString)
        } catch (e: IllegalArgumentException) {
            InteractionType.LIKE // Default value if conversion fails
        }
    }

    // ContentType converters
    @TypeConverter
    fun fromContentType(contentType: ContentType): String {
        return contentType.name
    }

    @TypeConverter
    fun toContentType(contentTypeString: String): ContentType {
        return try {
            ContentType.valueOf(contentTypeString)
        } catch (e: IllegalArgumentException) {
            ContentType.CHART // Default value if conversion fails
        }
    }
}
