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
                        Log.d(TAG, "Installed charts: $installedCharts")

                        if (shouldCheckForUpdates && installedCharts.isNotEmpty()) {
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

    fun fetchUpdates(installedCharts: List<Chart> = (localChartsState.value as? LocalChartsState.Success)?.charts ?: emptyList()) {
        if (installedCharts.isEmpty()) {
            _updatesState.value = UpdatesState.Success(emptyList())
            return
        }

        _updatesState.value = UpdatesState.Loading
        viewModelScope.launch {
            try {
                val latestVersionsResult = remoteChartRepository.getLatestVersionsByChartIds(installedCharts.map { it.id }).first()

                latestVersionsResult.fold(
                    onSuccess = { latestVersions ->
                        val updateInfoList = installedCharts.mapNotNull { localChart ->
                            val remoteVersion = latestVersions.find { it.chartId == localChart.id }
                                ?: return@mapNotNull null

                            val currentVersion = localChart.latestVersion.index
                            val availableVersion = remoteVersion.index

                            if (availableVersion > currentVersion) {
                                localChartRepository.updateChart(
                                    localChart.id,
                                    null,
                                    remoteVersion.index
                                )

                                localChart.copy(
                                    availableVersion = remoteVersion.index
                                )
                            } else null
                        }

                        _updatesState.value = UpdatesState.Success(updateInfoList)
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