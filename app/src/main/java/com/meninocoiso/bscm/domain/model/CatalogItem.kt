package com.meninocoiso.bscm.domain.model

import LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
sealed interface CatalogItem {
    val id: String
    val contentId: String
    val coverUrl: String
    val isFeatured: Boolean

    // Aggregated/derived fields useful for queries
    val downloadsSum: Int
    val isLiked: Boolean
    val isFavorited: Boolean
    
    // Device specific field, not from the API
    val isInstalled: Boolean?

    @Serializable(with = LocalDateTimeSerializer::class)
    val latestPublishedAt: LocalDateTime
}