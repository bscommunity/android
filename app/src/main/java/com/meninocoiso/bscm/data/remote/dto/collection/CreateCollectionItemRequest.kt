package com.meninocoiso.bscm.data.remote.dto.collection

import com.meninocoiso.bscm.domain.enums.ActionType
import com.meninocoiso.bscm.domain.enums.CollectionKind
import kotlinx.serialization.Serializable

@Serializable
data class CreateCollectionItemRequest(
    val contentId: String,
    val collectionId: String?,
    val collectionKind: CollectionKind,
    val action: ActionType
)

