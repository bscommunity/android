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

@HiltViewModel
class UpdatesViewModel @Inject constructor(
    @Named("Remote") private val remoteChartRepository: ChartRepository,
    @Named("Local") private val localChartRepository: ChartRepository
) : ViewModel() {
    private val _updatesState = MutableStateFlow<UpdatesState>(UpdatesState.Loading)
    val updatesState: StateFlow<UpdatesState> = _updatesState.asStateFlow()

    private val _localChartsState = MutableStateFlow<LocalChartsState>(LocalChartsState.Loading)
    val localChartsState: StateFlow<LocalChartsState> = _localChartsState.asStateFlow()

    /*init {
        loadLocalCharts()
    }*/

    fun loadLocalCharts() {
        _localChartsState.value = LocalChartsState.Loading
        viewModelScope.launch {
            localChartRepository.getCharts().collect { result ->
                _localChartsState.value = result.fold(
                    onSuccess = { charts ->
                        val installedCharts = charts.filter { it.isInstalled == true }

                        Log.d("UpdatesViewModel", "Installed charts: $installedCharts")

                        LocalChartsState.Success(installedCharts)
                    },
                    onFailure = { error -> LocalChartsState.Error(error.message) }
                )
            }
        }
    }

    fun fetchUpdates(installedCharts: List<Chart>) {
        _updatesState.value = UpdatesState.Loading
        viewModelScope.launch {
            try {
                val latestVersions = remoteChartRepository.getLatestVersionsByChartIds(installedCharts.map { it.id }).first().getOrNull()

                if (latestVersions == null) {
                    _updatesState.value = UpdatesState.Error("Failed to fetch latest versions")
                    return@launch
                }

                val chartsToUpdate = installedCharts.filter { chart ->
                    val remoteLatestVersion = latestVersions.find { it.chartId == chart.id }

                    return@filter remoteLatestVersion != null &&
                            remoteLatestVersion.id != chart.latestVersion.id
                }

                _updatesState.value = UpdatesState.Success(chartsToUpdate)
            } catch (e: Exception) {
                _updatesState.value = UpdatesState.Error(e.message)
            }
        }
    }
}