package com.meninocoiso.bscm.data.repository

import com.meninocoiso.bscm.data.local.dao.ChartDao
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

    override suspend fun getItemByContentId(contentId: String): Flow<Result<Chart>> = flow {
        val chart = chartDao.getChart(contentId)
        if (chart != null) {
            emit(Result.success(chart))
        } else {
            emit(Result.failure(IllegalArgumentException("Chart with contentId $contentId not found")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun getItems(ids: List<String>): Flow<Result<List<Chart>>> = flow {
        val charts = chartDao.getChartsByIds(ids)
        emit(Result.success(charts))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun getItemsByContentIds(contentIds: List<String>): Flow<Result<List<Chart>>> = flow {
        val charts = chartDao.getChartsByIds(contentIds)
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

}