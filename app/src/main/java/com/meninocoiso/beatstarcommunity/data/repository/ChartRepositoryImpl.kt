package com.meninocoiso.beatstarcommunity.data.repository

import com.meninocoiso.beatstarcommunity.data.remote.ApiClient
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ChartRepositoryImpl @Inject constructor(
    private val apiClient: ApiClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ChartRepository {
    override suspend fun getCharts(): Flow<Result<List<Chart>>> = flow {
        try {
            val users = apiClient.getCharts()
            emit(Result.success(users))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(dispatcher)

    override suspend fun getChart(id: String): Flow<Result<Chart>> = flow {
        try {
            val user = apiClient.getChart(id)
            emit(Result.success(user))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(dispatcher)
}