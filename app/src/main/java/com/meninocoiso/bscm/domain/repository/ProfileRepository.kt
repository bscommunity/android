package com.meninocoiso.bscm.domain.repository

import com.meninocoiso.bscm.data.remote.dto.activity.ActivityEntry
import com.meninocoiso.bscm.data.remote.dto.user.UserProfileResponse
import com.meninocoiso.bscm.domain.model.Chart

interface ProfileRepository {
    suspend fun getProfileHeader(userId: String, useCache: Boolean = true): Result<UserProfileResponse>
    suspend fun getProfileHeaderByUsername(username: String, useCache: Boolean = true): Result<UserProfileResponse>
    suspend fun getActivity(userId: String, limit: Int, offset: Int, useCache: Boolean = true): Result<List<ActivityEntry>>
    suspend fun getUserCharts(userId: String, limit: Int, offset: Int, useCache: Boolean = true): Result<List<Chart>>
    suspend fun followUser(userId: String): Result<Unit>
    suspend fun unfollowUser(userId: String): Result<Unit>
}
