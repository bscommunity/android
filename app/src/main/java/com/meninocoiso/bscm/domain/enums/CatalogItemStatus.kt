package com.meninocoiso.bscm.domain.enums

import kotlinx.serialization.Serializable

@Serializable
enum class CatalogItemStatus {
    DRAFT, PUBLISHED, ARCHIVED, REMOVED
}
