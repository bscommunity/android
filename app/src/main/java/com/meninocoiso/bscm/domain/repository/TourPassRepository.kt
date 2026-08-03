package com.meninocoiso.bscm.domain.repository

import com.meninocoiso.bscm.domain.model.TourPass
import kotlinx.coroutines.flow.Flow

/** Remote-only tour pass repository contracts. */
interface TourPassRemoteRepository {
    suspend fun getTourPasses(
        query: String? = null,
        limit: Int? = 10,
        offset: Int = 0,
    ): Flow<Result<List<TourPass>>>

    suspend fun getTourPass(id: String): Flow<Result<TourPass>>
}

/** Local-only tour pass repository contracts. */
interface TourPassLocalRepository {
    suspend fun getTourPasses(
        query: String? = null,
        limit: Int? = null,
        offset: Int = 0,
    ): Flow<Result<List<TourPass>>>

    suspend fun getTourPass(id: String): Flow<Result<TourPass>>

    fun observeTourPasses(): Flow<List<TourPass>>

    suspend fun insert(items: List<TourPass>): Flow<Result<Boolean>>

    suspend fun update(items: List<TourPass>): Flow<Result<Boolean>>

    suspend fun delete(items: List<TourPass>): Flow<Result<Boolean>>
}
