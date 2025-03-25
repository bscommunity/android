package com.meninocoiso.beatstarcommunity.data.manager

import android.util.Log
import com.meninocoiso.beatstarcommunity.data.repository.ChartRepository
import com.meninocoiso.beatstarcommunity.domain.enums.OperationType
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private const val TAG = "ChartManager"

/**
 * Result wrapper for chart operations
 */
sealed class FetchResult<out T> {
    data class Success<T>(val data: T) : FetchResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : FetchResult<Nothing>()
    data object Loading : FetchResult<Nothing>()
}

sealed class ChartState {
    data object Loading : ChartState()
    data object Success : ChartState()
    data object Error : ChartState()
}

// Event for one-time UI actions
sealed class FetchEvent {
    data class Error(val message: String) : FetchEvent()
}

/**
 * Singleton manager for chart data across the application
 */
@Singleton
class ChartManager @Inject constructor(
    @Named("Remote") private val remoteChartRepository: ChartRepository,
    @Named("Local") private val localChartRepository: ChartRepository,
) {
    private val _allCharts = MutableStateFlow<Map<String, Chart>>(emptyMap())
    private val allCharts: StateFlow<Map<String, Chart>> = _allCharts.asStateFlow()

    // Filtered chart collections
    val workshopCharts: Flow<List<Chart>> = allCharts.map { charts ->
        charts.values.toList()
    }

    val installedCharts: Flow<List<Chart>> = allCharts.map { charts ->
        charts.values.filter { it.isInstalled == true }.toList()
    }

    val chartsWithUpdates: Flow<List<Chart>> = allCharts.map { charts ->
        charts.values.filter {
            it.isInstalled == true && it.availableVersion != null
        }.toList()
    }

    private val _workshopState = MutableStateFlow<ChartState>(ChartState.Loading)
    val workshopState: StateFlow<ChartState> = _workshopState.asStateFlow()

    fun updateState(newState: ChartState) {
        _workshopState.value = newState
    }

    /**
     * Get the number of charts synchronously
     * This method uses the current value of the StateFlow to provide an immediate count
     */
    fun getChartsLength(): Int {
        return _allCharts.value.size
    }

    /**
     * Refresh charts from remote source
     */
    fun refreshRemoteCharts(query: String? = null, forceRefresh: Boolean = false): Flow<FetchResult<List<Chart>>> = flow {
        emit(FetchResult.Loading)

        if (!forceRefresh) {
            // Return cached charts if refresh isn't needed
            emit(FetchResult.Success(_allCharts.value.values.toList()))
            return@flow
        }

        try {
            val remoteResult = remoteChartRepository.getCharts(query).first()

            remoteResult.fold(
                onSuccess = { remoteCharts ->
                    Log.d(TAG, "Fetched ${remoteCharts.size} charts from remote")

                    // Update local cache
                    localChartRepository.insertCharts(remoteCharts).first()

                    // Update our in-memory cache
                    updateCharts(remoteCharts)

                    emit(FetchResult.Success(remoteCharts))
                    Log.d(TAG, "Updated ${remoteCharts.size} charts in memory")
                },
                onFailure = { error ->
                    emit(FetchResult.Error("Failed to fetch remote charts", error))
                    Log.e(TAG, "Failed to fetch remote charts", error)
                }
            )
        } catch (e: Exception) {
            emit(FetchResult.Error("Error refreshing charts", e))
            Log.e(TAG, "Error refreshing charts", e)
        }
    }.catch { e ->
        emit(FetchResult.Error("Unexpected error refreshing charts", e))
        Log.e(TAG, "Unexpected error refreshing charts", e)
    }

    /**
     * Load charts from local cache
     */
    suspend fun loadCachedCharts(): FetchResult<List<Chart>> {
        return try {
            val localResult = localChartRepository.getCharts().first()

            localResult.fold(
                onSuccess = { localCharts ->
                    Log.d(TAG, "Loaded ${localCharts.size} charts from local storage")
                    updateCharts(localCharts)
                    FetchResult.Success(localCharts)
                },
                onFailure = { error ->
                    FetchResult.Error("Failed to load cached charts", error)
                }
            )
        } catch (e: Exception) {
            FetchResult.Error("Error loading cached charts", e)
        }
    }

    /**
     * Check for updates to installed charts
     */
    fun checkForUpdates(): Flow<FetchResult<List<Chart>>> = flow {
        emit(FetchResult.Loading)

        val installedCharts = _allCharts.value.values
            .filter { it.isInstalled == true }
            .toList()

        if (installedCharts.isEmpty()) {
            emit(FetchResult.Success(emptyList<Chart>()))
            return@flow
        }

        try {
            val latestVersionsResult = remoteChartRepository
                .getLatestVersionsByChartIds(installedCharts.map { it.id })
                .first()

            latestVersionsResult.fold(
                onSuccess = { latestVersions ->
                    // Update charts that have newer versions available
                    val chartsToUpdate = mutableListOf<Chart>()

                    installedCharts.forEach { chart ->
                        val remoteVersion = latestVersions.find { it.chartId == chart.id }

                        if (remoteVersion != null &&
                            remoteVersion.index > chart.latestVersion.index) {

                            val updatedChart = chart.copy(availableVersion = remoteVersion)
                            updateChart(updatedChart)
                            chartsToUpdate.add(updatedChart)
                        }
                    }

                    // Update in local repository
                    if (chartsToUpdate.isNotEmpty()) {
                        localChartRepository.updateCharts(chartsToUpdate).first()
                    }

                    emit(FetchResult.Success(chartsToUpdate))
                    Log.d(TAG, "Updated ${chartsToUpdate.size} charts with new versions")
                },
                onFailure = { error ->
                    emit(FetchResult.Error("Failed to check for updates", error))
                }
            )
        } catch (e: Exception) {
            emit(FetchResult.Error("Error checking for updates", e))
        }
    }.catch { e ->
        emit(FetchResult.Error("Unexpected error checking for updates", e))
    }

    /**
     * Update a single chart in memory
     */
    private fun updateChart(chart: Chart) {
        _allCharts.value = _allCharts.value.toMutableMap().apply {
            this[chart.id] = chart
        }
    }

    /**
     * Update multiple charts in memory
     */
    private fun updateCharts(charts: List<Chart>) {
        val updatedMap = _allCharts.value.toMutableMap()

        charts.forEach { chart ->
            // Add to our map, preserving any existing data not in the update
            updatedMap[chart.id] = _allCharts.value[chart.id]?.let { existingChart ->
                // If we have this chart already, preserve certain fields
                // that might not be in the new data
                when {
                    // If chart is from remote, preserve installed status
                    chart.isInstalled == null -> chart.copy(
                        isInstalled = existingChart.isInstalled,
                        availableVersion = existingChart.availableVersion
                    )
                    // Otherwise take the new version completely
                    else -> chart
                }
            } ?: chart
        }

        _allCharts.value = updatedMap
    }

    /**
     * Update chart in memory with a specific operation
     */
    fun updateChart(chartId: String, operation: OperationType): Flow<FetchResult<List<Chart>>> = flow {
        // Update in local repository
        val result = localChartRepository.updateChart(chartId, operation).first()

        result.fold(
            onSuccess = { updated ->
                if (!updated) {
                    emit(FetchResult.Error("Failed to update chart in local storage"))
                    return@flow
                }
            },
            onFailure = { error ->
                emit(FetchResult.Error("Error updating chart in local storage", error))
                return@flow
            }
        )

        Log.d(TAG, "Updated chart with id: $chartId in local storage")

        // Find the chart in our in-memory cache
        val existingChart = _allCharts.value[chartId] ?: run {
            Log.d(TAG, "Chart with id: $chartId not found")
            emit(FetchResult.Error("Chart not found"))
            return@flow
        }

        // Update in-memory cache
        when (operation) {
            OperationType.INSTALL -> {
                updateChart(existingChart.copy(isInstalled = true))
            }
            OperationType.UPDATE -> {
                if (existingChart.availableVersion == null) {
                    emit(FetchResult.Error("No available version to update"))
                    return@flow
                }
                updateChart(existingChart.copy(
                    latestVersion = existingChart.availableVersion!!,
                    availableVersion = null,
                ))
            }
            OperationType.DELETE -> {
                updateChart(existingChart.copy(isInstalled = false))
            }
        }

        Log.d(TAG, "Updated chart with id: $chartId in memory")

        // Only emit a single success result
        emit(FetchResult.Success(_allCharts.value.values.toList()))
    }.catch { e ->
        emit(FetchResult.Error("Unexpected error updating chart", e))
    }
}