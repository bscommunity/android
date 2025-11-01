package com.meninocoiso.bscm.domain.model

import LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import org.bscm.serialization.UUIDSerializer
import java.time.LocalDateTime
import java.util.UUID

@Serializable
data class Collection(
    val id: ULong,
    @Serializable(with = UUIDSerializer::class)
    val userId: UUID,
    val name: String,
    val isPublic: Boolean,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updatedAt: LocalDateTime,
    val itemCount: Int = 0,
    val items: List<CatalogItem>? = null
)