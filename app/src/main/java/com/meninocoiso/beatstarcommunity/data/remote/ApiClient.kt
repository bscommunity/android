package com.meninocoiso.beatstarcommunity.data.remote

import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.domain.model.User
import com.meninocoiso.beatstarcommunity.domain.model.Version

interface ApiClient {
    suspend fun getUsers(): List<User>
    suspend fun getUser(id: String): User
    suspend fun getChart(id: String): Chart
    suspend fun getCharts(): List<Chart>
    suspend fun getChartsById(ids: List<String>): List<Chart>
    suspend fun getLatestVersionsByChartIds(ids: List<String>): List<Version>
}