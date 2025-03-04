package com.meninocoiso.beatstarcommunity.data.repository

import com.meninocoiso.beatstarcommunity.domain.model.Chart
import kotlinx.coroutines.flow.Flow

interface ChartRepository {
    suspend fun getCharts(): Flow<Result<List<Chart>>>
    suspend fun insertCharts(charts: List<Chart>): Flow<Result<Boolean>>
    suspend fun updateChart(chart: Chart): Flow<Result<Boolean>>
    suspend fun updateChart(id: String, isInstalled: Boolean?): Flow<Result<Boolean>>
    suspend fun getChart(id: String): Flow<Result<Chart>>
    suspend fun deleteChart(chart: Chart): Flow<Result<Boolean>>
}