package com.meninocoiso.beatstarcommunity.data.repository

import com.meninocoiso.beatstarcommunity.data.remote.ApiClient
import com.meninocoiso.beatstarcommunity.domain.enums.Difficulty
import com.meninocoiso.beatstarcommunity.domain.enums.Genre
import com.meninocoiso.beatstarcommunity.domain.enums.OperationType
import com.meninocoiso.beatstarcommunity.domain.enums.SortOption
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
    override suspend fun getCharts(
        query: String?,
        difficulties: List<Difficulty>?,
        genres: List<Genre>?,
        limit: Int?,
        offset: Int
    ): Flow<Result<List<Chart>>> {
        return flow {
            try {
                val charts = apiClient.getCharts(query, difficulties, genres, limit, offset)
                emit(Result.success(charts))
            } catch (e: Exception) {
                emit(Result.failure(e))
            }
        }.flowOn(dispatcher)
    }

    override suspend fun getChartsSortedBy(
        sortBy: SortOption,
        limit: Int?,
        offset: Int
    ): Flow<Result<List<Chart>>> {
        return flow {
            try {
                val charts = apiClient.getFeedCharts(sortBy, limit, offset)
                emit(Result.success(charts))
            } catch (e: Exception) {
                emit(Result.failure(e))
            }
        }.flowOn(dispatcher)
    }

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
    
    override suspend fun getSuggestions(query: String, limit: Int?): Flow<Result<List<String>>> = flow {
        try {
            val suggestions = apiClient.getSuggestions(query, limit)
            emit(Result.success(suggestions))
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

    override suspend fun getInstallStatus(id: String): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun insertCharts(charts: List<Chart>): Flow<Result<Boolean>> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteChart(chart: Chart): Flow<Result<Boolean>> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteCharts(charts: List<Chart>): Flow<Result<Boolean>> {
        TODO("Not yet implemented")
    }

    override suspend fun updateChart(chart: Chart): Flow<Result<Boolean>> {
        TODO("Not yet implemented")
    }

    override suspend fun updateChart(
        id: String,
        operation: OperationType,
    ): Flow<Result<Boolean>> {
        TODO("Not yet implemented")
    }

    override suspend fun updateCharts(charts: List<Chart>): Flow<Result<Boolean>> {
        TODO("Not yet implemented")
    }
}