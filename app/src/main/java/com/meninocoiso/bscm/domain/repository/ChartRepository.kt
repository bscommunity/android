package com.meninocoiso.bscm.domain.repository

import com.meninocoiso.bscm.domain.enums.Difficulty
import com.meninocoiso.bscm.domain.enums.Genre
import com.meninocoiso.bscm.domain.enums.OperationOption
import com.meninocoiso.bscm.domain.enums.SortOption
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Version
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
        operation: OperationOption = OperationOption.INSTALL,
    ): Flow<Result<Boolean>>
    suspend fun deleteChart(chart: Chart): Flow<Result<Boolean>>
    suspend fun deleteChart(id: String): Flow<Result<Boolean>>
    suspend fun deleteCharts(charts: List<Chart>): Flow<Result<Boolean>>
    suspend fun postAnalytics(
        id: String,
        operation: OperationOption,
    ): Flow<Result<Boolean>>
}