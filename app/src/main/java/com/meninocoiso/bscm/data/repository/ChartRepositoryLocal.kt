package com.meninocoiso.bscm.data.repository

import android.util.Log
import com.meninocoiso.bscm.data.local.dao.ChartDao
import com.meninocoiso.bscm.domain.enums.Difficulty
import com.meninocoiso.bscm.domain.enums.Genre
import com.meninocoiso.bscm.domain.enums.OperationType
import com.meninocoiso.bscm.domain.enums.SortOption
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Version
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
                SortOption.LAST_UPDATED -> chartDao.getChartsSortedByLastUpdated(limit, offset)
                else -> chartDao.getAll(null, limit, offset)
            }
            emit(Result.success(charts))
        }.catch { e ->
            emit(Result.failure(e))
        }.flowOn(dispatcher)
    }
    
    override suspend fun getSuggestions(query: String, limit: Int?): Flow<Result<List<String>>> = flow {
        emit(Result.success(chartDao.getSuggestions(query, limit)))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun getChart(id: String): Flow<Result<Chart>> = flow {
        val chart = chartDao.getChart(id)
        if (chart != null) {
            emit(Result.success(chart))
        } else {
            emit(Result.failure(IllegalArgumentException("Chart with id $id not found")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun getInstallStatus(id: String): Boolean {
        return chartDao.getChart(id)?.isInstalled == true
    }

    override suspend fun getLatestVersionsByChartIds(ids: List<String>): Flow<Result<List<Version>>> = flow {
        emit(Result.success(chartDao.getLatestVersionsByChartIds(ids)))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun insertCharts(charts: List<Chart>): Flow<Result<Boolean>> = flow {
        chartDao.insert(charts)
        // Log.d(TAG, "Charts after insertion: ${chartDao.getAll()}")
        emit(Result.success(true))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun updateChart(chart: Chart): Flow<Result<Boolean>> = flow {
        chartDao.update(chart)
        Log.d(TAG, "Updated chart: ${chartDao.getChart(chart.id)}")
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
        Log.d(TAG, "Updated charts locally: ${chartDao.getAll()}")
        emit(Result.success(true))
    }.catch { e->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun deleteChart(chart: Chart): Flow<Result<Boolean>> = flow<Result<Boolean>> {
        chartDao.delete(chart)
        emit(Result.success(true))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun deleteChart(id: String): Flow<Result<Boolean>> = flow<Result<Boolean>> {
        val chart = chartDao.getChart(id)
        if (chart != null) {
            chartDao.delete(chart)
            emit(Result.success(true))
        } else {
            emit(Result.failure(IllegalArgumentException("Chart with id $id not found")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun deleteCharts(charts: List<Chart>): Flow<Result<Boolean>> = flow<Result<Boolean>> {
        chartDao.delete(charts)
        emit(Result.success(true))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun postAnalytics(
        id: String,
        operation: OperationType
    ): Flow<Result<Boolean>> {
        TODO("Not yet implemented")
    }
}