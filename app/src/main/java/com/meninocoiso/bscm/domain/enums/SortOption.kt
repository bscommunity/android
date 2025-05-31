package com.meninocoiso.bscm.domain.enums

import kotlinx.serialization.Serializable

@Serializable
enum class SortOption {
    WEEKLY_RANK,
    LAST_UPDATED,
    MOST_DOWNLOADED,
    MOST_LIKED
}