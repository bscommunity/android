package com.meninocoiso.bscm.domain.model

import com.meninocoiso.bscm.domain.enums.CatalogItemStatus
import com.meninocoiso.bscm.domain.enums.CatalogItemType
import com.meninocoiso.bscm.domain.enums.Visibility
import com.meninocoiso.bscm.domain.serialization.LocalDateTimeSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.JsonClassDiscriminator
import java.time.LocalDateTime

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("itemKind")
sealed interface CatalogItem {
    val id: String

    val type: CatalogItemType
    val status: CatalogItemStatus
    val visibility: Visibility

    val isFeatured: Boolean

    val downloadsSum: Int

    val contributors: List<Contributor>

    val createdAt: LocalDateTime
    val publishedAt: LocalDateTime?
    val updatedAt: LocalDateTime?

    val likedAt: LocalDateTime?
    val bookmarkedAt: LocalDateTime?

    val isInstalled: Boolean?

    val previewVideoId: String?

    val discordChannelId: String?
    val discordMessageId: String?

    val authorId: String?
}
