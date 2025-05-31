package com.meninocoiso.beatstarcommunity.data.repository

import com.meninocoiso.beatstarcommunity.domain.enums.Difficulty
import com.meninocoiso.beatstarcommunity.domain.enums.Genre
import com.meninocoiso.beatstarcommunity.domain.enums.OperationType
import com.meninocoiso.beatstarcommunity.domain.enums.SortOption
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.domain.model.Version
import kotlinx.coroutines.flow.Flow

interface ChartRepository {
    suspend fun getCharts(
        query: String? = null,
        difficulties: List<Difficulty>? = null,
        genres: List<Genre>? = null,
        limit: Int? = 10,
        offset: Int = 0
    ): Flow<Result<List<Chart>>>
    suspend fun getChartsSortedBy(
        sortBy: SortOption,
        limit: Int? = 10,
        offset: Int = 0,
    ): Flow<Result<List<Chart>>>
    suspend fun getChart(id: String): Flow<Result<Chart>>
    suspend fun getSuggestions(query: String, limit: Int? = null): Flow<Result<List<String>>>
    suspend fun getInstallStatus(id: String): Boolean
    suspend fun getLatestVersionsByChartIds(ids: List<String>): Flow<Result<List<Version>>>
    suspend fun insertCharts(charts: List<Chart>): Flow<Result<Boolean>>
    suspend fun updateChart(chart: Chart): Flow<Result<Boolean>>
    suspend fun updateCharts(charts: List<Chart>): Flow<Result<Boolean>>
    suspend fun updateChart(
        id: String,
        operation: OperationType = OperationType.INSTALL,
    ): Flow<Result<Boolean>>
    suspend fun deleteChart(chart: Chart): Flow<Result<Boolean>>
    suspend fun deleteCharts(charts: List<Chart>): Flow<Result<Boolean>>
    suspend fun postAnalytics(
        chartId: String,
        action: OperationType,
    ): Flow<Result<Boolean>>
}