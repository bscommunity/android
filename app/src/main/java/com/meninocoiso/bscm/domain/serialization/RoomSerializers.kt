package com.meninocoiso.bscm.domain.serialization

import androidx.room.TypeConverter
import com.meninocoiso.bscm.domain.enums.ActionType
import com.meninocoiso.bscm.domain.enums.CollectionKind
import com.meninocoiso.bscm.domain.enums.ContentType
import com.meninocoiso.bscm.domain.enums.Difficulty
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Contributor
import com.meninocoiso.bscm.domain.model.KnownIssue
import com.meninocoiso.bscm.domain.model.StreamingLink
import com.meninocoiso.bscm.domain.model.Version
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class RoomSerializers {
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
    
    // String List converters
    @TypeConverter
    fun fromStringList(strings: List<String>): String {
        return json.encodeToString(strings)
    }
    
    @TypeConverter
    fun toStringList(stringsString: String): List<String> {
        return if (stringsString.isBlank()) {
            emptyList()
        } else {
            json.decodeFromString(stringsString)
        }
    }

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

    // Contributors List converters
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

    // Charts List converters
    @TypeConverter
    fun fromChartsList(charts: List<Chart>): String {
        return json.encodeToString(charts)
    }

    @TypeConverter
    fun toChartsList(chartsString: String): List<Chart> {
        return if (chartsString.isBlank()) {
            emptyList()
        } else {
            json.decodeFromString(chartsString)
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

    // ActionType converters
    @TypeConverter
    fun fromActionType(value: ActionType): String {
        return value.name
    }

    @TypeConverter
    fun toActionType(value: String): ActionType {
        return ActionType.valueOf(value)
    }

    @TypeConverter
    fun fromCollectionKind(value: CollectionKind): String {
        return value.name
    }

    @TypeConverter
    fun toCollectionKind(value: String): CollectionKind {
        return CollectionKind.valueOf(value)
    }

    @TypeConverter
    fun fromContentType(value: ContentType): String {
        return value.name
    }

    @TypeConverter
    fun toContentType(value: String): ContentType {
        return ContentType.valueOf(value)
    }
}
