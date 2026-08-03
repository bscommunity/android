package com.meninocoiso.bscm.domain.repository

import com.meninocoiso.bscm.domain.enums.Difficulty
import com.meninocoiso.bscm.domain.enums.Genre
import com.meninocoiso.bscm.domain.enums.SortOption
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Version
import kotlinx.coroutines.flow.Flow

/** Optional query filters for charts. */
data class ChartQuery(
    val difficulties: List<Difficulty>? = null,
    val genres: List<Genre>? = null,
) : ContentQuery

/** Chart-specific version operations. */
interface ChartVersionRepository {
    suspend fun getLatestVersionsByChartIds(ids: List<String>): Flow<Result<List<Version>>>
}

/** Remote-only chart repository contracts. */
interface ChartRemoteRepository :
    ContentFeedRepository<Chart, SortOption, ChartQuery>,
    ContentItemRepository<Chart>,
    ContentSuggestionsRepository,
    ContentAnalyticsRepository,
    ChartVersionRepository

/** Local-only chart repository contracts. */
interface ChartLocalRepository :
    ContentLocalRepository<Chart, SortOption, ChartQuery>,
    ContentItemRepository<Chart> {

    /**
     * Live single-chart stream: re-emits whenever the local chart row changes
     * (like/bookmark timestamps, install flag), unlike [ContentItemRepository.getItem]
     * which is a one-shot read.
     */
    fun observeItem(id: String): Flow<Chart?>
}