package com.meninocoiso.beatstarcommunity.domain.model.internal

import com.meninocoiso.beatstarcommunity.domain.enums.SortOption

data class Cache(
    val searchHistory: List<String> = emptyList(),
    val folderUri: String? = null,
    val latestWorkshopSort: SortOption = SortOption.LAST_UPDATED,
)