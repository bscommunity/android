package com.meninocoiso.bscm.domain.model

import com.meninocoiso.bscm.domain.enums.CollectionKind
import com.meninocoiso.bscm.domain.serialization.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class Collection(
    val id: String,
    val userId: String,
    val kind: CollectionKind,
    val name: String,
    val isPublic: Boolean,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime,
    val items: List<CatalogItem> = emptyList(),
    val coverUrl: String? = null,
    val itemCount: Int = 0,
)