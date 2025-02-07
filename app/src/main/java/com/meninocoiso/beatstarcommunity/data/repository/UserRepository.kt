package com.meninocoiso.beatstarcommunity.data.repository

import com.meninocoiso.beatstarcommunity.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getUsers(): Flow<Result<List<User>>>
    suspend fun getUser(id: String): Flow<Result<User>>
}