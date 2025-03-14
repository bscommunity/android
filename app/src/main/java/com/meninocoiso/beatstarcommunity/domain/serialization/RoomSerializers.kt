package com.meninocoiso.beatstarcommunity.domain.serialization

import androidx.room.TypeConverter
import com.meninocoiso.beatstarcommunity.domain.enums.DifficultyEnum
import com.meninocoiso.beatstarcommunity.domain.model.Contributor
import com.meninocoiso.beatstarcommunity.domain.model.KnownIssue
import com.meninocoiso.beatstarcommunity.domain.model.StreamingLink
import com.meninocoiso.beatstarcommunity.domain.model.Version
import kotlinx.serialization.json.Json
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): LocalDate? {
        return value?.let { LocalDate.ofEpochDay(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: LocalDate?): Long? {
        return date?.toEpochDay()
    }

    // Chart
    private val json = Json { ignoreUnknownKeys = true }

    // StreamingLink List converters
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
    fun fromDifficultyEnum(difficulty: DifficultyEnum): String {
        return difficulty.name
    }

    @TypeConverter
    fun toDifficultyEnum(difficultyString: String): DifficultyEnum {
        return try {
            DifficultyEnum.valueOf(difficultyString)
        } catch (e: IllegalArgumentException) {
            DifficultyEnum.Normal // Default value if conversion fails
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
}
