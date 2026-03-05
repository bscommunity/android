@file:UseSerializers(LocalDateTimeSerializer::class)

package com.meninocoiso.bscm.domain.model

import com.meninocoiso.bscm.domain.serialization.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDateTime

@Serializable
sealed interface CatalogItem {
    val id: String
    val contentId: String?
    val coverUrl: String
    val isFeatured: Boolean
    val contributors: List<Contributor>
    val downloadsSum: Int // Aggregated field

    val createdAt: LocalDateTime
    val updatedAt: LocalDateTime

    val likedAt: LocalDateTime? // Derived field
    val bookmarkedAt: LocalDateTime? // Derived field

    val isInstalled: Boolean? // Local database field
}