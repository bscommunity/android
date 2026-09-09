package com.meninocoiso.bscm.domain.serialization

import androidx.room.TypeConverter
import com.meninocoiso.bscm.data.remote.dto.user.SimplifiedUser
import com.meninocoiso.bscm.domain.enums.ActionType
import com.meninocoiso.bscm.domain.enums.CatalogItemStatus
import com.meninocoiso.bscm.domain.enums.CatalogItemType
import com.meninocoiso.bscm.domain.enums.CollectionKind
import com.meninocoiso.bscm.domain.enums.Difficulty
import com.meninocoiso.bscm.domain.model.Changelog
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Contributor
import com.meninocoiso.bscm.domain.model.StreamingRef
import com.meninocoiso.bscm.domain.model.Track
import com.meninocoiso.bscm.domain.model.Version
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class RoomSerializers {
    private val json = Json { ignoreUnknownKeys = true }

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

    @TypeConverter
    fun fromStringList(strings: List<String>): String {
        return json.encodeToString(strings)
    }

    @TypeConverter
    fun toStringList(stringsString: String): List<String> {
        return if (stringsString.isBlank()) emptyList()
        else json.decodeFromString(stringsString)
    }

    @TypeConverter
    fun fromStreamingRefList(streamingRefs: List<StreamingRef>): String {
        return json.encodeToString(streamingRefs)
    }

    @TypeConverter
    fun toStreamingRefList(streamingRefsString: String): List<StreamingRef> {
        return if (streamingRefsString.isBlank()) emptyList()
        else json.decodeFromString(streamingRefsString)
    }

    @TypeConverter
    fun fromTrack(track: Track): String {
        return json.encodeToString(track)
    }

    @TypeConverter
    fun toTrack(trackString: String): Track {
        return json.decodeFromString(trackString)
    }

    @TypeConverter
    fun fromContributorsList(contributors: List<Contributor>): String {
        return json.encodeToString(contributors)
    }

    @TypeConverter
    fun toContributorsList(contributorsString: String): List<Contributor> {
        return if (contributorsString.isBlank()) emptyList()
        else json.decodeFromString(contributorsString)
    }

    @TypeConverter
    fun fromChartsList(charts: List<Chart>): String {
        return json.encodeToString(charts)
    }

    @TypeConverter
    fun toChartsList(chartsString: String): List<Chart> {
        return if (chartsString.isBlank()) emptyList()
        else json.decodeFromString(chartsString)
    }

    @TypeConverter
    fun fromDifficultyEnum(difficulty: Difficulty): String {
        return difficulty.name
    }

    @TypeConverter
    fun toDifficultyEnum(difficultyString: String): Difficulty {
        return try {
            Difficulty.valueOf(difficultyString)
        } catch (e: IllegalArgumentException) {
            Difficulty.NORMAL
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
    fun fromCatalogItemType(value: CatalogItemType): String {
        return value.name
    }

    @TypeConverter
    fun toCatalogItemType(value: String): CatalogItemType {
        return CatalogItemType.valueOf(value)
    }

    @TypeConverter
    fun fromCatalogItemStatus(value: CatalogItemStatus): String {
        return value.name
    }

    @TypeConverter
    fun toCatalogItemStatus(value: String): CatalogItemStatus {
        return CatalogItemStatus.valueOf(value)
    }

    @TypeConverter
    fun fromChangelogList(changelogs: List<Changelog>): String {
        return json.encodeToString(changelogs)
    }

    @TypeConverter
    fun toChangelogList(changelogsString: String): List<Changelog> {
        return if (changelogsString.isBlank()) emptyList()
        else json.decodeFromString(changelogsString)
    }

    @TypeConverter
    fun fromSimplifiedUser(user: SimplifiedUser): String {
        return json.encodeToString(user)
    }

    @TypeConverter
    fun toSimplifiedUser(userString: String): SimplifiedUser {
        return json.decodeFromString(userString)
    }
}
