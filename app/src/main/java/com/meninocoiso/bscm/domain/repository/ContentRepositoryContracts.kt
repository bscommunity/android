package com.meninocoiso.bscm.domain.repository

import com.meninocoiso.bscm.domain.enums.OperationOption
import kotlinx.coroutines.flow.Flow

/** Base query marker for content-specific filters. */
sealed interface ContentQuery {
    data object None : ContentQuery
}

/** Read-only feed/search operations for content. */
interface ContentFeedRepository<T, S, Q : ContentQuery> {
    suspend fun getContent(
        query: String? = null,
        sortBy: S? = null,
        limit: Int? = 10,
        offset: Int = 0,
        filters: Q? = null,
    ): Flow<Result<List<T>>>
}

/** Local cache mutations for content. */
interface ContentCacheRepository<T> {
    suspend fun insert(items: List<T>): Flow<Result<Boolean>>
    suspend fun update(items: List<T>): Flow<Result<Boolean>>
    suspend fun delete(items: List<T>): Flow<Result<Boolean>>
}

/** Local repository that supports feed/search + cache mutations. */
interface ContentLocalRepository<T, S, Q : ContentQuery> :
    ContentFeedRepository<T, S, Q>,
    ContentCacheRepository<T>,
    ContentOperationRepository

/** Single item access. */
interface ContentItemRepository<T> {
    suspend fun getItem(id: String): Flow<Result<T>>
}

/** Suggestions/search hints for content. */
interface ContentSuggestionsRepository {
    suspend fun getSuggestions(query: String, limit: Int? = null): Flow<Result<List<String>>>
}

/** Operations triggered by local install/update/delete actions. */
interface ContentOperationRepository {
    suspend fun updateContent(
        id: String,
        operation: OperationOption = OperationOption.INSTALL,
    ): Flow<Result<Boolean>>
}

/** Remote analytics posting. */
interface ContentAnalyticsRepository {
    suspend fun postAnalytics(
        id: String,
        operation: OperationOption,
    ): Flow<Result<Boolean>>
}
