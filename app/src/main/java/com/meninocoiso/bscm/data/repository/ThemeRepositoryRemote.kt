package com.meninocoiso.bscm.data.repository

import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.domain.model.Theme
import com.meninocoiso.bscm.domain.repository.ThemeRemoteRepository
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ThemeRepositoryRemote @Inject constructor(
    private val apiClient: ApiClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ThemeRemoteRepository {
    override suspend fun getThemes(
        query: String?,
        limit: Int?,
        offset: Int
    ): Flow<Result<List<Theme>>> = flow {
        val themes = apiClient.getThemes(
            query = query,
            limit = limit,
            offset = offset
        )
        emit(Result.success(themes))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun getTheme(id: String): Flow<Result<Theme>> = flow {
        val theme = apiClient.getTheme(id)
        emit(Result.success(theme))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)
}
