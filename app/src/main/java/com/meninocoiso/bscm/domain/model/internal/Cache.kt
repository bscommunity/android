package com.meninocoiso.bscm.domain.model.internal

import com.meninocoiso.bscm.data.remote.dto.user.SimplifiedUser
import com.meninocoiso.bscm.domain.enums.SortOption

data class Cache(
    val searchHistory: List<String> = emptyList(),
    val latestWorkshopSort: SortOption = SortOption.LAST_UPDATED,
    val user: SimplifiedUser? = null,
)