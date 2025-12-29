package com.meninocoiso.bscm.domain.repository

import kotlinx.coroutines.flow.Flow

/** Generic repository contract mirroring chart operations for reuse. */
interface ContentRepository<T> {
    suspend fun getSorted(sortBy: Any, limit: Int? = 10, offset: Int = 0): Flow<Result<List<T>>>
    suspend fun search(query: String, limit: Int? = 10, offset: Int = 0): Flow<Result<List<T>>>
    suspend fun update(items: List<T>): Flow<Result<Boolean>>
    suspend fun delete(items: List<T>): Flow<Result<Boolean>>
}

