package com.meninocoiso.beatstarcommunity.data.repository

import com.meninocoiso.beatstarcommunity.domain.model.Chart
import kotlinx.coroutines.flow.Flow

interface ChartRepository {
    suspend fun getCharts(): Flow<Result<List<Chart>>>
    suspend fun getChart(id: String): Flow<Result<Chart>>
    suspend fun deleteChart(chart: Chart): Flow<Result<Boolean>>
}