package com.meninocoiso.beatstarcommunity.data.repository

import android.util.Log
import com.meninocoiso.beatstarcommunity.data.local.dao.ChartDao
import com.meninocoiso.beatstarcommunity.domain.enums.OperationType
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.domain.model.Version
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

private const val TAG = "ChartRepositoryLocal"

class ChartRepositoryLocal(
    private val chartDao: ChartDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ChartRepository {
    override suspend fun getCharts(query: String?): Flow<Result<List<Chart>>> = flow {
        try {
            emit(Result.success(chartDao.getAll()))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(dispatcher)

    override suspend fun getChartsById(ids: List<String>): Flow<Result<List<Chart>>> = flow {
        try {
            emit(Result.success(chartDao.loadAllByIds(ids)))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(dispatcher)

    override suspend fun getLatestVersionsByChartIds(ids: List<String>): Flow<Result<List<Version>>> = flow {
        try {
            emit(Result.success(chartDao.getLatestVersionsByChartIds(ids)))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(dispatcher)

    override suspend fun getChart(id: String): Flow<Result<Chart>> = flow {
        try {
            emit(Result.success(chartDao.getChart(id)))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(dispatcher)

    override suspend fun getInstallStatus(id: String): Boolean {
        return chartDao.getChart(id).isInstalled == true
    }

    override suspend fun insertCharts(charts: List<Chart>): Flow<Result<Boolean>> = flow {
        try {
            chartDao.insert(charts)

            // Log.d(TAG, "Charts after insertion: ${chartDao.getAll()}")

            emit(Result.success(true))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(dispatcher)

    override suspend fun updateChart(chart: Chart): Flow<Result<Boolean>> = flow {
        try {
            chartDao.update(chart)
            emit(Result.success(true))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(dispatcher)

    override suspend fun updateChart(
        id: String,
        operation: OperationType,
    ): Flow<Result<Boolean>> = flow {
        Log.d("Current chart", chartDao.getChart(id).toString())

        try {
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

            Log.d(TAG, "Updated chart with: ${chartDao.getChart(id)}")

            emit(Result.success(true))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(dispatcher)

    override suspend fun updateCharts(charts: List<Chart>): Flow<Result<Boolean>> = flow {
        try {
            chartDao.update(charts)

            // Log.d(TAG, "Updated charts: ${chartDao.getAll()}")

            emit(Result.success(true))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(dispatcher)

    override suspend fun deleteChart(chart: Chart): Flow<Result<Boolean>> = flow<Result<Boolean>> {
        try {
            chartDao.delete(chart)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(dispatcher)

    override suspend fun deleteCharts(charts: List<Chart>): Flow<Result<Boolean>> = flow<Result<Boolean>> {
        try {
            chartDao.delete(charts)
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(dispatcher)
}