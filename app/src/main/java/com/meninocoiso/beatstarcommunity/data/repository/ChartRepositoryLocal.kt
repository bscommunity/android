package com.meninocoiso.beatstarcommunity.data.repository

import android.util.Log
import com.meninocoiso.beatstarcommunity.data.local.dao.ChartDao
import com.meninocoiso.beatstarcommunity.domain.enums.Difficulty
import com.meninocoiso.beatstarcommunity.domain.enums.Genre
import com.meninocoiso.beatstarcommunity.domain.enums.OperationType
import com.meninocoiso.beatstarcommunity.domain.enums.SortOption
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.domain.model.Version
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

private const val TAG = "ChartRepositoryLocal"

class ChartRepositoryLocal(
    private val chartDao: ChartDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ChartRepository {
    override suspend fun getCharts(
        query: String?,
        difficulties: List<Difficulty>?,
        genres: List<Genre>?,
        limit: Int?,
        offset: Int
    ): Flow<Result<List<Chart>>> = flow {
        val charts = chartDao.getAll(query, limit, offset)
        emit(Result.success(charts))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun getChartsSortedBy(
        sortBy: SortOption,
        limit: Int?,
        offset: Int
    ): Flow<Result<List<Chart>>> {
        return flow {
            val charts = when (sortBy) {
                SortOption.MOST_DOWNLOADED -> chartDao.getChartsSortedByMostDownloaded(limit, offset)
                else -> chartDao.getChartsSortedByLastUpdated(limit, offset)
            }
            emit(Result.success(charts))
        }.catch { e ->
            emit(Result.failure(e))
        }.flowOn(dispatcher)
    }
    
    override suspend fun getChartsById(ids: List<String>): Flow<Result<List<Chart>>> = flow {
        emit(Result.success(chartDao.loadAllByIds(ids)))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun getLatestVersionsByChartIds(ids: List<String>): Flow<Result<List<Version>>> = flow {
        emit(Result.success(chartDao.getLatestVersionsByChartIds(ids)))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)
    
    override suspend fun getSuggestions(query: String, limit: Int?): Flow<Result<List<String>>> = flow {
        emit(Result.success(chartDao.getSuggestions(query, limit)))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun getChart(id: String): Flow<Result<Chart>> = flow {
         emit(Result.success(chartDao.getChart(id)))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun getInstallStatus(id: String): Boolean {
        return chartDao.getChart(id).isInstalled == true
    }

    override suspend fun insertCharts(charts: List<Chart>): Flow<Result<Boolean>> = flow {
        chartDao.insert(charts)
        // Log.d(TAG, "Charts after insertion: ${chartDao.getAll()}")
        emit(Result.success(true))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun updateChart(chart: Chart): Flow<Result<Boolean>> = flow {
        chartDao.update(chart)
        emit(Result.success(true))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun updateChart(
        id: String,
        operation: OperationType
    ): Flow<Result<Boolean>> = flow {
        // Log.d(TAG, "Current chart: ${chartDao.getChart(id)}")

        when (operation) {
            OperationType.INSTALL -> {
                Log.d(TAG, "Updating data from chart with id: $id")
                chartDao.update(id, true)
            }
            OperationType.UPDATE -> {
                Log.d(TAG, "Updating chart with id: $id")
                chartDao.updateVersion(id)
            }
            OperationType.DELETE -> {
                Log.d(TAG, "Deleting chart with id: $id")
                chartDao.update(id, false)
            }
        }

        // Log.d(TAG, "Updated chart: ${chartDao.getChart(id)}")
        emit(Result.success(true))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun updateCharts(charts: List<Chart>): Flow<Result<Boolean>> = flow {
        // Log.d(TAG, "Updating charts: $charts")
        chartDao.update(charts)
        // Log.d(TAG, "Updated charts locally: ${chartDao.getAll()}")
        emit(Result.success(true))
    }.catch { e->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun deleteChart(chart: Chart): Flow<Result<Boolean>> = flow<Result<Boolean>> {
        chartDao.delete(chart)
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun deleteCharts(charts: List<Chart>): Flow<Result<Boolean>> = flow<Result<Boolean>> {
        chartDao.delete(charts)
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun postAnalytics(
        chartId: String,
        action: OperationType
    ): Flow<Result<Boolean>> {
        TODO("Not yet implemented")
    }
}