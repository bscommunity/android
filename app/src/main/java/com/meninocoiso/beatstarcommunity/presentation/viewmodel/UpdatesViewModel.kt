package com.meninocoiso.beatstarcommunity.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.beatstarcommunity.data.repository.ChartRepository
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

sealed class UpdatesState {
    data object Loading : UpdatesState()
    data class Success(val charts: List<Chart>) : UpdatesState()
    data class Error(val message: String?) : UpdatesState()
}

sealed class LocalChartsState {
    data object Loading : LocalChartsState()
    data class Success(val charts: List<Chart>) : LocalChartsState()
    data class Error(val message: String?) : LocalChartsState()
}

private const val TAG = "UpdatesViewModel"

@HiltViewModel
class UpdatesViewModel @Inject constructor(
    @Named("Remote") private val remoteChartRepository: ChartRepository,
    @Named("Local") private val localChartRepository: ChartRepository
) : ViewModel() {
    private val _updatesState = MutableStateFlow<UpdatesState>(UpdatesState.Success(emptyList()))
    val updatesState: StateFlow<UpdatesState> = _updatesState.asStateFlow()

    private val _localChartsState = MutableStateFlow<LocalChartsState>(LocalChartsState.Loading)
    val localChartsState: StateFlow<LocalChartsState> = _localChartsState.asStateFlow()

    init {
        loadLocalCharts(true)
    }

    fun loadLocalCharts(shouldCheckForUpdates: Boolean = false) {
        _localChartsState.value = LocalChartsState.Loading
        viewModelScope.launch {
            localChartRepository.getCharts().collect { result ->
                _localChartsState.value = result.fold(
                    onSuccess = { charts ->
                        val installedCharts = charts.filter { it.isInstalled == true }

                        if (shouldCheckForUpdates && installedCharts.size > 1) {
                            Log.d(TAG, "Fetching updates for installed charts")
                            fetchUpdates(installedCharts)
                        }

                        LocalChartsState.Success(installedCharts)
                    },
                    onFailure = { error -> LocalChartsState.Error(error.message) }
                )
            }
        }
    }

    fun fetchUpdates(
        installedCharts: List<Chart>? = null,
        chartToRemove: String? = null
    ) {
        if (chartToRemove != null) {
            if (_updatesState.value is UpdatesState.Success) {
                val updatedCharts = (_updatesState.value as UpdatesState.Success).charts.filter { it.id != chartToRemove }
                _updatesState.value = UpdatesState.Success(updatedCharts)
            }
            Log.d(TAG, "Removing chart $chartToRemove from updates list")
            return
        }

        val charts = installedCharts ?: (_localChartsState.value as? LocalChartsState.Success)?.charts

        if (charts.isNullOrEmpty()) {
            _updatesState.value = UpdatesState.Success(emptyList())
            Log.d(TAG, "No installed charts to check for updates")
            return
        }

        _updatesState.value = UpdatesState.Loading
        viewModelScope.launch {
            try {
                val latestVersionsResult = remoteChartRepository.getLatestVersionsByChartIds(charts.map { it.id }).first()

                latestVersionsResult.fold(
                    onSuccess = { latestVersions ->
                        val updatesList = charts.mapNotNull { localChart ->
                            val remoteVersion = latestVersions.find { it.chartId == localChart.id }
                                ?: return@mapNotNull null

                            val currentVersion = localChart.latestVersion.index
                            val availableVersion = remoteVersion.index

                            // Log.d(TAG, "Checking for updates for ${localChart.id}: current=$currentVersion, available=$availableVersion")

                            if (availableVersion > currentVersion) {
                                localChart.copy(
                                    availableVersion = remoteVersion
                                )
                            } else null
                        }

                        localChartRepository.updateCharts(updatesList).collect {
                            _updatesState.value = UpdatesState.Success(updatesList)
                            Log.d(TAG, "Updates fetched successfully")
                        }
                    },
                    onFailure = { error ->
                        _updatesState.value = UpdatesState.Error(error.message)
                    }
                )
            } catch (e: Exception) {
                _updatesState.value = UpdatesState.Error(e.message)
            }
        }
    }
}