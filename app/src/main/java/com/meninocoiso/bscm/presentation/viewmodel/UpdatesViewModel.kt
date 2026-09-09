package com.meninocoiso.bscm.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.data.manager.ChartManager
import com.meninocoiso.bscm.data.manager.ThemeManager
import com.meninocoiso.bscm.data.manager.TourPassManager
import com.meninocoiso.bscm.data.manager.TourPassStorageManager
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Theme
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.domain.result.ContentResult
import com.meninocoiso.bscm.domain.result.ContentState
import com.meninocoiso.bscm.util.StorageUtils
import com.meninocoiso.bscm.util.StorageUtils.BEATSTAR_URI
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "UpdatesViewModel"

@HiltViewModel
class UpdatesViewModel @Inject constructor(
    private val chartManager: ChartManager,
    private val tourPassManager: TourPassManager,
    private val themeManager: ThemeManager,
    private val tourPassStorageManager: TourPassStorageManager,
    @param:ApplicationContext private val context: Context
) : ViewModel() {
    val pendingUpdateCharts: Flow<List<Chart>> = chartManager.pendingUpdateCharts
    val installedCharts: Flow<List<Chart>> = chartManager.installedCharts
    val cachedThemes: Flow<List<Theme>> = themeManager.cachedThemes

    /**
     * Tour passes marked as installed by the manager's installed-ids signal
     * (the same source of truth the details screens use), resolved against
     * the cached tour pass data. The cache also holds tour passes the user
     * merely viewed (feed/search visits) or deleted, so only tour passes the
     * manager still considers installed are emitted.
     */
    val installedTourPasses: Flow<List<TourPass>> = combine(
        tourPassManager.cachedTourPasses,
        tourPassManager.installedTourPassIds
    ) { tourPasses, installedIds ->
        tourPasses
            .filter { it.id in installedIds }
            .map { tourPass -> tourPass.copy(isInstalled = true) }
    }

    val cacheState = chartManager.cacheState

    private val _updateState = MutableStateFlow<ContentState>(ContentState.Loading)
    val updateState: StateFlow<ContentState> = _updateState.asStateFlow()

    init {
        viewModelScope.launch {
            checkForUpdates(false)
        }
    }

    /**
     * Check for updates to installed charts
     * @param showLoading Whether to show loading cacheState or keep showing existing data
     */
    fun checkForUpdates(showLoading: Boolean = true) {
        viewModelScope.launch {
            // If we want to show loading cacheState, update the UI
            if (showLoading) {
                _updateState.value = ContentState.Loading
            }

            // Check for updates
            chartManager.checkForUpdates().collect { result ->
                when (result) {
                    is ContentResult.Success -> {
                        _updateState.value = ContentState.Success
                    }
                    is ContentResult.Error -> {
                        _updateState.value = ContentState.Error
                    }
                    ContentResult.Loading -> {
                        // Already handled above if showLoading is true
                    }
                }
            }
        }
    }

    /**
     * Scans local storage for installed charts
     * Called after storage permission is granted
     */
    fun scanLocalCharts() {
        viewModelScope.launch {
            val rootUri = StorageUtils.getFolderUri(context, BEATSTAR_URI)
            if (rootUri != null) {
                // Hydrate tour passes recorded in the root manifest (survives app
                // uninstalls), then scan local chart folders.
                tourPassStorageManager.hydrateInstalledTourPasses()
                chartManager.scanLocalCharts(rootUri)
            }
        }
    }
}