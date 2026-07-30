package com.meninocoiso.bscm.data.remote.dto.collection

import com.meninocoiso.bscm.domain.enums.ActionType
import com.meninocoiso.bscm.domain.enums.CollectionKind
import com.meninocoiso.bscm.domain.serialization.LocalDateTimeSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class BatchCollectionItemRequest(
    val catalogId: String,
    val collectionId: String?,
    val collectionKind: CollectionKind,
    val action: ActionType,
    @Serializable(with = LocalDateTimeSerializer::class)
    val enqueuedAt: LocalDateTime? = null
)