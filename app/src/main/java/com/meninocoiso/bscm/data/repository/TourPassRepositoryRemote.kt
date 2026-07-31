package com.meninocoiso.bscm.data.repository

import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.domain.repository.TourPassRemoteRepository
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class TourPassRepositoryRemote @Inject constructor(
    private val apiClient: ApiClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : TourPassRemoteRepository {
    override suspend fun getTourPasses(
        query: String?,
        limit: Int?,
        offset: Int
    ): Flow<Result<List<TourPass>>> = flow {
        val tourPasses = apiClient.getTourPasses(
            query = query,
            limit = limit,
            offset = offset
        )
        emit(Result.success(tourPasses))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun getTourPass(id: String): Flow<Result<TourPass>> = flow {
        val tourPass = apiClient.getTourPass(id)
        emit(Result.success(tourPass))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)
}
