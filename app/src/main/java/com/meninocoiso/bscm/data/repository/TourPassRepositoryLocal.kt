package com.meninocoiso.bscm.data.repository

import com.meninocoiso.bscm.data.local.dao.TourPassDao
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.domain.repository.TourPassLocalRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class TourPassRepositoryLocal(
    private val tourPassDao: TourPassDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : TourPassLocalRepository {
    override suspend fun getTourPasses(
        query: String?,
        limit: Int?,
        offset: Int
    ): Flow<Result<List<TourPass>>> = flow {
        val tourPasses = tourPassDao.getTourPasses(query, limit, offset)
        emit(Result.success(tourPasses))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun getTourPass(id: String): Flow<Result<TourPass>> = flow {
        val tourPass = tourPassDao.getTourPass(id)
        if (tourPass != null) {
            emit(Result.success(tourPass))
        } else {
            emit(Result.failure(IllegalArgumentException("Tour pass with id $id not found")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun insert(items: List<TourPass>): Flow<Result<Boolean>> = flow {
        tourPassDao.insert(items)
        emit(Result.success(true))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun update(items: List<TourPass>): Flow<Result<Boolean>> = flow {
        tourPassDao.update(items)
        emit(Result.success(true))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun delete(items: List<TourPass>): Flow<Result<Boolean>> = flow {
        tourPassDao.delete(items)
        emit(Result.success(true))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)
}
