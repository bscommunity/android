package com.meninocoiso.bscm.domain.model.internal

import com.meninocoiso.bscm.domain.enums.SortOption
import com.meninocoiso.bscm.domain.model.User

data class Cache(
    val searchHistory: List<String> = emptyList(),
    val latestWorkshopSort: SortOption = SortOption.LAST_UPDATED,
    val user: User? = null,
)