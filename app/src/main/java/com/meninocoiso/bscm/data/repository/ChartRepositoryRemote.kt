package com.meninocoiso.bscm.data.repository

import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.domain.enums.OperationOption
import com.meninocoiso.bscm.domain.enums.SortOption
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Version
import com.meninocoiso.bscm.domain.repository.ChartQuery
import com.meninocoiso.bscm.domain.repository.ChartRemoteRepository
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
) : ChartRemoteRepository {
    override suspend fun getContent(
        query: String?,
        sortBy: SortOption?,
        limit: Int?,
        offset: Int,
        filters: ChartQuery?
    ): Flow<Result<List<Chart>>> = flow {
        val charts = apiClient.getCharts(
            query = query,
            sortBy = sortBy,
            difficulties = filters?.difficulties,
            genres = filters?.genres,
            limit = limit,
            offset = offset
        )
        emit(Result.success(charts))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun getItem(id: String): Flow<Result<Chart>> = flow {
        val chart = apiClient.getChart(id)
        emit(Result.success(chart))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun getItemByContentId(contentId: String): Flow<Result<Chart>> {
        return flow {
            val chart = apiClient.getChartByContentId(contentId)
            emit(Result.success(chart))
        }.catch { e ->
            emit(Result.failure(e))
        }.flowOn(dispatcher)
    }
    
    override suspend fun getItems(ids: List<String>): Flow<Result<List<Chart>>> = flow {
        val charts = apiClient.getChartsByIds(ids)
        emit(Result.success(charts))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun getItemsByContentIds(contentIds: List<String>): Flow<Result<List<Chart>>> = flow {
        val charts = apiClient.getChartsByContentIds(contentIds)
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

    override suspend fun getSuggestions(query: String, limit: Int?): Flow<Result<List<String>>> =
        flow {
            val suggestions = apiClient.getSuggestions(query, limit)
            emit(Result.success(suggestions))
        }.catch { e ->
            emit(Result.failure(e))
        }.flowOn(dispatcher)
    
    override suspend fun postAnalytics(
        id: String,
        operation: OperationOption,
    ): Flow<Result<Boolean>> = flow {
        val result = apiClient.postAnalytics(id, operation)
        emit(Result.success(result))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)
}