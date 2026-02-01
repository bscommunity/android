package com.meninocoiso.bscm.data.remote

import com.meninocoiso.bscm.domain.enums.Difficulty
import com.meninocoiso.bscm.domain.enums.Genre
import com.meninocoiso.bscm.domain.enums.OperationOption
import com.meninocoiso.bscm.domain.enums.SortOption
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.User
import com.meninocoiso.bscm.domain.model.Version
import com.meninocoiso.bscm.domain.model.auth.AuthRequest
import com.meninocoiso.bscm.domain.model.auth.AuthResponse
import com.meninocoiso.bscm.domain.model.auth.RefreshTokenRequest
import com.meninocoiso.bscm.domain.model.internal.ContributionCategory

interface ApiClient {
    suspend fun getChart(id: String): Chart
    suspend fun getCharts(
        query: String?,
        sortBy: SortOption? = null,
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
        operationOption: OperationOption
    ): Boolean

    // Authentication methods
    suspend fun authenticateWithDiscord(authRequest: AuthRequest): AuthResponse
    suspend fun refreshToken(refreshRequest: RefreshTokenRequest): AuthResponse
    suspend fun getCurrentUser(): User

    // Utils methods
    suspend fun getContributors(): List<ContributionCategory>
}
