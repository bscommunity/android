package com.meninocoiso.bscm.domain.repository

import com.meninocoiso.bscm.data.remote.dto.activity.ActivityEntry
import com.meninocoiso.bscm.data.remote.dto.user.UserProfileResponse
import com.meninocoiso.bscm.domain.model.Chart

interface MeRepository {
    suspend fun getProfile(useCache: Boolean = true): Result<UserProfileResponse>
    suspend fun getActivity(limit: Int, offset: Int, useCache: Boolean = true): Result<List<ActivityEntry>>
    suspend fun getLikes(limit: Int, offset: Int, useCache: Boolean = true): Result<List<Chart>>
    suspend fun getBookmarks(limit: Int, offset: Int, useCache: Boolean = true): Result<List<Chart>>
}
