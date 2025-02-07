package com.meninocoiso.beatstarcommunity.data.repository

import com.meninocoiso.beatstarcommunity.data.remote.ApiClient
import com.meninocoiso.beatstarcommunity.domain.model.User
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class UserRepositoryImpl @Inject constructor(
    private val apiClient: ApiClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : UserRepository {
    override suspend fun getUsers(): Flow<Result<List<User>>> = flow {
        try {
            val users = apiClient.getUsers()
            emit(Result.success(users))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(dispatcher)

    override suspend fun getUser(id: String): Flow<Result<User>> = flow {
        try {
            val user = apiClient.getUser(id)
            emit(Result.success(user))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(dispatcher)
}