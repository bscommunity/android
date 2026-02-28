package com.meninocoiso.bscm.domain.repository

import com.meninocoiso.bscm.data.remote.dto.activity.ActivityItemResponse
import com.meninocoiso.bscm.data.remote.dto.user.UserProfileResponse
import com.meninocoiso.bscm.domain.model.Chart

interface ProfileRepository {
    suspend fun getProfileHeader(username: String, useCache: Boolean = true): Result<UserProfileResponse>
    suspend fun getActivity(userId: String, limit: Int, offset: Int, useCache: Boolean = true): Result<List<ActivityItemResponse>>
    suspend fun getUserCharts(userId: String, limit: Int, offset: Int, useCache: Boolean = true): Result<List<Chart>>
    suspend fun followUser(userId: String, username: String): Result<Unit>
    suspend fun unfollowUser(userId: String, username: String): Result<Unit>
}
