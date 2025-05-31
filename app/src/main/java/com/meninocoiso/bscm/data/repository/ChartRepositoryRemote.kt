package com.meninocoiso.bscm.data.repository

import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.domain.enums.Difficulty
import com.meninocoiso.bscm.domain.enums.Genre
import com.meninocoiso.bscm.domain.enums.OperationType
import com.meninocoiso.bscm.domain.enums.SortOption
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Version
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
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
    ): Flow<Result<List<Chart>>> = flow {
        val charts = apiClient.getCharts(query, difficulties, genres, limit, offset)
        emit(Result.success(charts))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun getLatestVersionsByChartIds(ids: List<String>): Flow<Result<List<Version>>> =
        flow {
            val charts = apiClient.getLatestVersionsByChartIds(ids)
            emit(Result.success(charts))
        }.catch { e ->
            emit(Result.failure(e))
        }.flowOn(dispatcher)

    override suspend fun getChartsSortedBy(
        sortBy: SortOption,
        limit: Int?,
        offset: Int
    ): Flow<Result<List<Chart>>> = flow {
        val charts = apiClient.getFeedCharts(sortBy, limit, offset)
        emit(Result.success(charts))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun getSuggestions(query: String, limit: Int?): Flow<Result<List<String>>> =
        flow {
            val suggestions = apiClient.getSuggestions(query, limit)
            emit(Result.success(suggestions))
        }.catch { e ->
            emit(Result.failure(e))
        }.flowOn(dispatcher)

    override suspend fun getChart(id: String): Flow<Result<Chart>> = flow {
        val chart = apiClient.getChart(id)
        emit(Result.success(chart))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun postAnalytics(
        id: String,
        operation: OperationType,
    ): Flow<Result<Boolean>> = flow {
        val result = apiClient.postAnalytics(id, operation)
        emit(Result.success(result))
    }.catch { e ->
        emit(Result.failure(e))
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