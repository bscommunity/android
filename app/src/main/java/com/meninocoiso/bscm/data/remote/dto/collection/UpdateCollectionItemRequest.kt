package com.meninocoiso.bscm.data.remote.dto.collection

import com.meninocoiso.bscm.domain.enums.ActionType
import kotlinx.serialization.Serializable

@Serializable
data class UpdateCollectionItemRequest(
    val contentId: String,
    val collectionId: String, // "likes", "favorites", or a custom collection ID
    val action: ActionType
)

