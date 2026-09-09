package com.meninocoiso.bscm.domain.repository

import com.meninocoiso.bscm.domain.model.Theme
import kotlinx.coroutines.flow.Flow

/** Remote-only theme repository contracts. */
interface ThemeRemoteRepository {
    suspend fun getThemes(
        query: String? = null,
        limit: Int? = 10,
        offset: Int = 0,
    ): Flow<Result<List<Theme>>>

    suspend fun getTheme(id: String): Flow<Result<Theme>>
}

/** Local-only theme repository contracts. */
interface ThemeLocalRepository {
    suspend fun getThemes(
        query: String? = null,
        limit: Int? = null,
        offset: Int = 0,
    ): Flow<Result<List<Theme>>>

    suspend fun getTheme(id: String): Flow<Result<Theme>>

    fun observeThemes(): Flow<List<Theme>>

    suspend fun insert(items: List<Theme>): Flow<Result<Boolean>>

    suspend fun update(items: List<Theme>): Flow<Result<Boolean>>

    suspend fun delete(items: List<Theme>): Flow<Result<Boolean>>
}
