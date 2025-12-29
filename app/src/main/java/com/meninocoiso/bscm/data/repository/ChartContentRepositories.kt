package com.meninocoiso.bscm.data.repository

import com.meninocoiso.bscm.domain.enums.SortOption
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.repository.ChartRepository
import com.meninocoiso.bscm.domain.repository.ContentRepository
import javax.inject.Inject
import javax.inject.Named

/** Adapter that exposes [ChartRepository] as [ContentRepository] for remote usage. */
class ChartContentRepositoryRemote @Inject constructor(
    @param:Named("Remote") private val chartRepository: ChartRepository
) : ContentRepository<Chart> {
    override suspend fun getSorted(sortBy: Any, limit: Int?, offset: Int): kotlinx.coroutines.flow.Flow<Result<List<Chart>>> {
        val option = sortBy as? SortOption ?: SortOption.LAST_UPDATED
        return chartRepository.getChartsSortedBy(option, limit, offset)
    }

    override suspend fun search(query: String, limit: Int?, offset: Int): kotlinx.coroutines.flow.Flow<Result<List<Chart>>> {
        return chartRepository.getCharts(query = query, limit = limit, offset = offset)
    }

    // Remote repository is not used for cache mutations in ContentManager
    override suspend fun update(items: List<Chart>): kotlinx.coroutines.flow.Flow<Result<Boolean>> =
        kotlinx.coroutines.flow.flow { emit(Result.success(true)) }

    override suspend fun delete(items: List<Chart>): kotlinx.coroutines.flow.Flow<Result<Boolean>> =
        kotlinx.coroutines.flow.flow { emit(Result.success(true)) }
}

/** Adapter that exposes [ChartRepository] as [ContentRepository] for local/cache usage. */
class ChartContentRepositoryLocal @Inject constructor(
    @param:Named("Local") private val chartRepository: ChartRepository
) : ContentRepository<Chart> {
    override suspend fun getSorted(sortBy: Any, limit: Int?, offset: Int): kotlinx.coroutines.flow.Flow<Result<List<Chart>>> {
        val option = sortBy as? SortOption ?: SortOption.LAST_UPDATED
        return chartRepository.getChartsSortedBy(option, limit, offset)
    }

    override suspend fun search(query: String, limit: Int?, offset: Int): kotlinx.coroutines.flow.Flow<Result<List<Chart>>> {
        return chartRepository.getCharts(query = query, limit = limit, offset = offset)
    }

    override suspend fun update(items: List<Chart>): kotlinx.coroutines.flow.Flow<Result<Boolean>> =
        chartRepository.updateCharts(items)

    override suspend fun delete(items: List<Chart>): kotlinx.coroutines.flow.Flow<Result<Boolean>> =
        chartRepository.deleteCharts(items)
}

