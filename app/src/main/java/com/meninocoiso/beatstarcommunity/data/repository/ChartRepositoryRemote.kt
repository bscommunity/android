package com.meninocoiso.beatstarcommunity.data.repository

import com.meninocoiso.beatstarcommunity.data.remote.ApiClient
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.domain.model.Version
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ChartRepositoryRemote @Inject constructor(
    private val apiClient: ApiClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ChartRepository {
    override suspend fun getCharts(): Flow<Result<List<Chart>>> = flow {
        try {
            val charts = apiClient.getCharts()
            emit(Result.success(charts))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(dispatcher)

    override suspend fun getChartsById(ids: List<String>): Flow<Result<List<Chart>>> = flow {
        try {
            val charts = apiClient.getChartsById(ids)
            emit(Result.success(charts))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(dispatcher)

    override suspend fun getLatestVersionsByChartIds(ids: List<String>): Flow<Result<List<Version>>> = flow {
        try {
            val charts = apiClient.getLatestVersionsByChartIds(ids)
            emit(Result.success(charts))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(dispatcher)

    override suspend fun getChart(id: String): Flow<Result<Chart>> = flow {
        try {
            val chart = apiClient.getChart(id)
            emit(Result.success(chart))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(dispatcher)

    override suspend fun insertCharts(charts: List<Chart>): Flow<Result<Boolean>> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteChart(chart: Chart): Flow<Result<Boolean>> {
        TODO("Not yet implemented")
    }

    override suspend fun updateChart(chart: Chart): Flow<Result<Boolean>> {
        TODO("Not yet implemented")
    }

    override suspend fun updateChart(id: String, isInstalled: Boolean?): Flow<Result<Boolean>> {
        TODO("Not yet implemented")
    }
}