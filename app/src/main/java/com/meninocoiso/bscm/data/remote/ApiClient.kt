package com.meninocoiso.bscm.data.remote

import com.meninocoiso.bscm.domain.enums.Difficulty
import com.meninocoiso.bscm.domain.enums.Genre
import com.meninocoiso.bscm.domain.enums.OperationType
import com.meninocoiso.bscm.domain.enums.SortOption
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.User
import com.meninocoiso.bscm.domain.model.Version

interface ApiClient {
    suspend fun getUsers(): List<User>
    suspend fun getUser(id: String): User
    suspend fun getChart(id: String): Chart
    suspend fun getFeedCharts(sortBy: SortOption, limit: Int? = 10, offset: Int = 0): List<Chart>
    suspend fun getCharts(
        query: String?,
        difficulties: List<Difficulty>? = null,
        genres: List<Genre>? = null,
        limit: Int? = 10,
        offset: Int = 0
    ): List<Chart>
    suspend fun getChartsById(ids: List<String>): List<Chart>
    suspend fun getSuggestions(query: String, limit: Int? = null): List<String>
    suspend fun getLatestVersionsByChartIds(ids: List<String>): List<Version>
    suspend fun postAnalytics(
        id: String,
        operationType: OperationType
    ): Boolean
}