package com.meninocoiso.bscm.data.repository

import android.util.Log
import com.meninocoiso.bscm.data.local.dao.ChartDao
import com.meninocoiso.bscm.domain.enums.OperationOption
import com.meninocoiso.bscm.domain.enums.SortOption
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.repository.ChartLocalRepository
import com.meninocoiso.bscm.domain.repository.ChartQuery
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.time.LocalDateTime

private const val TAG = "ChartRepositoryLocal"

class ChartRepositoryLocal(
    private val chartDao: ChartDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ChartLocalRepository {
    override suspend fun getContent(
        query: String?,
        sortBy: SortOption?,
        limit: Int?,
        offset: Int,
        filters: ChartQuery?
    ): Flow<Result<List<Chart>>> = flow {
        val charts = when (sortBy) {
            SortOption.MOST_DOWNLOADED -> chartDao.getChartsSortedByMostDownloadedWithQuery(query, limit, offset)
            SortOption.LAST_UPDATED -> chartDao.getChartsSortedByLastUpdatedWithQuery(query, limit, offset)
            else -> chartDao.getAll(query, limit, offset)
        }
        emit(Result.success(charts))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun getItem(id: String): Flow<Result<Chart>> = flow {
        val chart = chartDao.getChart(id)
        if (chart != null) {
            emit(Result.success(chart))
        } else {
            emit(Result.failure(IllegalArgumentException("Chart with id $id not found")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun getItemsById(ids: List<String>): Flow<Result<List<Chart>>> = flow {
        val charts = chartDao.getChartsByIds(ids)
        emit(Result.success(charts))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun insert(items: List<Chart>): Flow<Result<Boolean>> = flow {
        chartDao.insert(items)
        emit(Result.success(true))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun update(items: List<Chart>): Flow<Result<Boolean>> = flow {
        chartDao.update(items)
        emit(Result.success(true))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun delete(items: List<Chart>): Flow<Result<Boolean>> = flow {
        chartDao.delete(items)
        emit(Result.success(true))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun updateContentStatus(
        id: String,
        operation: OperationOption
    ): Flow<Result<Boolean>> = flow {
        when (operation) {
            OperationOption.INSTALL -> {
                Log.d(TAG, "Updating data from chart with id: $id")
                chartDao.update(id, true)
            }
            OperationOption.UPDATE -> {
                Log.d(TAG, "Updating chart with id: $id")
                chartDao.updateVersion(id)
            }
            OperationOption.DELETE -> {
                Log.d(TAG, "Deleting chart with id: $id")
                chartDao.update(id, false)
            }
            OperationOption.LIKE -> {
                val now = LocalDateTime.now().toString()
                Log.d(TAG, "Liking chart with id: $id at $now")
                chartDao.updateLikedAt(id, now)
            }
            OperationOption.UNLIKE -> {
                Log.d(TAG, "Unliking chart with id: $id")
                chartDao.updateLikedAt(id, null)
            }
            OperationOption.BOOKMARK -> {
                val now = LocalDateTime.now().toString()
                Log.d(TAG, "Bookmarking chart with id: $id at $now")
                chartDao.updateBookmarkedAt(id, now)
            }
            OperationOption.UNBOOKMARK -> {
                Log.d(TAG, "Unbookmarking chart with id: $id")
                chartDao.updateBookmarkedAt(id, null)
            }
        }

        emit(Result.success(true))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    /**
     * Update the like status for a chart locally
     * Persists to database immediately
     */
    override suspend fun updateLikedAt(id: String, likedAt: String?) {
        chartDao.updateLikedAt(id, likedAt)
        Log.d(TAG, "Updated likedAt for chart $id: $likedAt")
    }

    /**
     * Update the bookmark status for a chart locally
     * Persists to database immediately
     */
    override suspend fun updateBookmarkedAt(id: String, bookmarkedAt: String?) {
        chartDao.updateBookmarkedAt(id, bookmarkedAt)
        Log.d(TAG, "Updated bookmarkedAt for chart $id: $bookmarkedAt")
    }
}