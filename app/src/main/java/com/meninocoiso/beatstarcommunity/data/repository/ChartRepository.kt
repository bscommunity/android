package com.meninocoiso.beatstarcommunity.data.repository

import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.domain.model.Version
import kotlinx.coroutines.flow.Flow

interface ChartRepository {
    suspend fun getCharts(): Flow<Result<List<Chart>>>
    suspend fun getChartsById(ids: List<String>): Flow<Result<List<Chart>>>
    suspend fun getLatestVersionsByChartIds(ids: List<String>): Flow<Result<List<Version>>>
    suspend fun getChart(id: String): Flow<Result<Chart>>
    suspend fun getInstallStatus(id: String): Boolean
    suspend fun insertCharts(charts: List<Chart>): Flow<Result<Boolean>>
    suspend fun updateChart(chart: Chart): Flow<Result<Boolean>>
    suspend fun updateChart(
        id: String,
        isInstalled: Boolean?,
        availableVersion: Int? = null,
    ): Flow<Result<Boolean>>
    suspend fun deleteChart(chart: Chart): Flow<Result<Boolean>>
}