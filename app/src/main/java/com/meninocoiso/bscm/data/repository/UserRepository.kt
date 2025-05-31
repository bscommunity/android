package com.meninocoiso.bscm.data.repository

import com.meninocoiso.bscm.domain.model.User
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getUsers(): Flow<Result<List<User>>>
    suspend fun getUser(id: String): Flow<Result<User>>
}