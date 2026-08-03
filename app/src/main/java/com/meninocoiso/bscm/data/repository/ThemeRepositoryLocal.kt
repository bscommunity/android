package com.meninocoiso.bscm.data.repository

import com.meninocoiso.bscm.data.local.dao.ThemeDao
import com.meninocoiso.bscm.domain.model.Theme
import com.meninocoiso.bscm.domain.repository.ThemeLocalRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class ThemeRepositoryLocal(
    private val themeDao: ThemeDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : ThemeLocalRepository {
    override suspend fun getThemes(
        query: String?,
        limit: Int?,
        offset: Int
    ): Flow<Result<List<Theme>>> = flow {
        val themes = themeDao.getThemes(query, limit, offset)
        emit(Result.success(themes))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun getTheme(id: String): Flow<Result<Theme>> = flow {
        val theme = themeDao.getTheme(id)
        if (theme != null) {
            emit(Result.success(theme))
        } else {
            emit(Result.failure(IllegalArgumentException("Theme with id $id not found")))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override fun observeThemes(): Flow<List<Theme>> = themeDao.observeThemes()

    override suspend fun insert(items: List<Theme>): Flow<Result<Boolean>> = flow {
        themeDao.insert(items)
        emit(Result.success(true))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun update(items: List<Theme>): Flow<Result<Boolean>> = flow {
        themeDao.update(items)
        emit(Result.success(true))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)

    override suspend fun delete(items: List<Theme>): Flow<Result<Boolean>> = flow {
        themeDao.delete(items)
        emit(Result.success(true))
    }.catch { e ->
        emit(Result.failure(e))
    }.flowOn(dispatcher)
}
