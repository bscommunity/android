package com.meninocoiso.bscm.domain.repository

import com.meninocoiso.bscm.data.remote.dto.activity.ActivityItemResponse
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.presentation.viewmodel.profile.PagedResult
import kotlinx.coroutines.flow.Flow

interface MeRepository {
    suspend fun getActivity(limit: Int, offset: Int, useCache: Boolean): Result<List<ActivityItemResponse>>

    suspend fun getLikes(limit: Int, offset: Int, useCache: Boolean): Result<PagedResult<CatalogItem>>

    suspend fun getBookmarks(limit: Int, offset: Int, useCache: Boolean): Result<PagedResult<CatalogItem>>

    // -----------------------------------------------------------------
    // Reactive streams — backed by Room so they emit on every local
    // write, including those made by InteractionViewModel.
    // The ViewModel collects these to keep the profile list live.
    // -----------------------------------------------------------------

    /**
     * A hot stream of all liked content (charts, tour passes, themes) from the
     * local DB. Emits a new list automatically whenever a like/unlike happens.
     */
    fun observeLikes(): Flow<List<CatalogItem>>

    /**
     * A hot stream of all bookmarked content (charts, tour passes, themes) from
     * the local DB. Emits a new list automatically whenever a bookmark changes.
     */
    fun observeBookmarks(): Flow<List<CatalogItem>>
}