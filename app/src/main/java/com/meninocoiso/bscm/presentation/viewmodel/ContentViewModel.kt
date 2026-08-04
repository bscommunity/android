package com.meninocoiso.bscm.presentation.viewmodel

import DownloadEvent
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.manager.ChartManager
import com.meninocoiso.bscm.data.manager.TourPassManager
import com.meninocoiso.bscm.data.manager.TourPassStorageManager
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.data.repository.DownloadRepository
import com.meninocoiso.bscm.data.repository.SettingsRepository
import com.meninocoiso.bscm.domain.enums.ErrorType
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.domain.model.internal.Settings
import com.meninocoiso.bscm.domain.repository.ChartLocalRepository
import com.meninocoiso.bscm.domain.state.DownloadState
import com.meninocoiso.bscm.monitor.DownloadServiceMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.io.IOException
import javax.inject.Inject

private const val TAG = "ContentViewModel"

@HiltViewModel
class ContentViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val apiClient: ApiClient,
    private val downloadServiceMonitor: DownloadServiceMonitor,
    private val downloadRepository: DownloadRepository,
    private val chartManager: ChartManager,
    private val tourPassManager: TourPassManager,
    private val tourPassStorageManager: TourPassStorageManager,
    private val chartLocalRepository: ChartLocalRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _downloadStates = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    private val downloadStates: StateFlow<Map<String, DownloadState>> = _downloadStates.asStateFlow()

    // Event flow for one-time notifications
    private val _events = MutableSharedFlow<DownloadEvent>(
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<DownloadEvent> = _events.asSharedFlow()

    // Track operation status to prevent concurrent operations on same chart
    private val chartOperations = mutableMapOf<String, String>()
    private val chartOperationsLock = Mutex()

    val isGameplayVideoPreviewEnabled = settingsRepository.settingsFlow
        .map { it.enableGameplayPreviewVideo }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Settings().enableGameplayPreviewVideo
        )

    val isExplicitContentAllowed = settingsRepository.settingsFlow
        .map { it.allowExplicitContent }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Settings().allowExplicitContent
        )

    // Cached setting read by download/aggregation logic (which is not a composable).
    @Volatile
    private var allowExplicitContent = Settings().allowExplicitContent

    init {
        observeDownloadEvents()
        observeSettings()
        observeInstalledCharts()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                allowExplicitContent = settings.allowExplicitContent
            }
        }
    }

    /**
     * Seeds the download state of every chart the user has already installed,
     * straight from the in-memory content store. Because the store is a
     * StateFlow-backed cache, this reflects already-installed charts on the
     * very first frame of a details screen, without waiting for per-chart
     * database reads.
     */
    private fun observeInstalledCharts() {
        viewModelScope.launch {
            chartManager.installedCharts.collect { charts ->
                charts.forEach { chart ->
                    if (chart.isInstalled == true && _downloadStates.value[chart.id] == null) {
                        updateState(chart.id, DownloadState.Installed(chart.id))
                    }
                }
            }
        }
    }

    private fun observeDownloadEvents() {
        viewModelScope.launch {
            downloadRepository.downloadEvents.collect { event ->
                handleDownloadEvent(event)
            }
        }
    }

    /**
     * Checks the installation status of a chart
     */
    fun checkStatus(chart: Chart) {
        viewModelScope.launch {
            try {
                val isInstalled = chart.isInstalled == true
                val isDownloadActive = downloadServiceMonitor.isDownloadActive(chart.id)

                val state = when {
                    isDownloadActive -> {
                        // Check what type of download is active
                        val activeDownloads = downloadServiceMonitor.getActiveDownloads()
                        if (chart.id in activeDownloads) {
                            DownloadState.Downloading(chart.id, 0f) // Will be updated by events
                        } else {
                            DownloadState.Idle
                        }
                    }
                    isInstalled -> DownloadState.Installed(chart.id)
                    else -> DownloadState.Idle
                }

                updateState(chart.id, state)
                // Log.d(TAG, "Status checked for chart ${chart.id}: $state")
            } catch (e: Exception) {
                Log.e(TAG, "Error checking chart status for ${chart.id}", e)
                updateState(chart.id, DownloadState.Error(
                    chart.id,
                    "Failed to check chart status",
                    ErrorType.UNKNOWN
                ))
            }
        }
    }

    /**
     * Merges the locally persisted state of a chart (like/bookmark timestamps,
     * install flag) into the chart object currently being displayed. Charts
     * embedded in tour passes come from the tour pass payload and carry no
     * interaction state, so without this merge the details screen would show
     * charts as never liked/bookmarked/downloaded even when they are.
     *
     * The underlying stream is live (Room re-emits on row changes), so the
     * merged chart follows like/unlike and bookmark toggles made anywhere in
     * the app, exactly like the bookmark membership flow. The flow is cached
     * per chart id: recreating it on every recomposition would re-emit the
     * un-merged payload chart first (making the like button flicker).
     */
    private val chartStateCache = object :
        LinkedHashMap<String, StateFlow<Chart>>(16, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, StateFlow<Chart>>
        ) = size > 10
    }

    fun observeChartState(chart: Chart): StateFlow<Chart> =
        chartStateCache.getOrPut(chart.id) {
            chartLocalRepository.observeItem(chart.id)
                .map { stored ->
                    stored?.let { local ->
                        chart.copy(
                            likedAt = local.likedAt ?: chart.likedAt,
                            bookmarkedAt = local.bookmarkedAt ?: chart.bookmarkedAt,
                            isInstalled = if (local.isInstalled == true) true else chart.isInstalled,
                        )
                    } ?: chart
                }
                .distinctUntilChanged()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = chart
                )
        }

    private suspend fun handleDownloadEvent(event: DownloadEvent) {
        val chartId = event.id

        try {
            // Update the state based on the event
            when (event) {
                is DownloadEvent.Started ->
                    updateState(chartId, DownloadState.Downloading(chartId, 0f))

                is DownloadEvent.Progress ->
                    updateState(chartId, DownloadState.Downloading(chartId, event.progress))

                is DownloadEvent.Extracting ->
                    updateState(chartId, DownloadState.Extracting(chartId, event.progress))

                is DownloadEvent.Complete -> {
                    updateState(chartId, DownloadState.Installed(chartId))
                    emitEvent(event)
                    // Clear any pending operations
                    clearChartOperation(chartId)
                }

                is DownloadEvent.Error -> {
                    updateState(chartId, DownloadState.Error(chartId, event.message, event.type))
                    emitEvent(event)
                    // Clear any pending operations
                    clearChartOperation(chartId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling download event: $event", e)
        }
    }

    private suspend fun clearChartOperation(chartId: String) {
        chartOperationsLock.withLock {
            chartOperations.remove(chartId)
        }
    }

    private suspend fun setChartOperation(chartId: String, operation: String): Boolean {
        return chartOperationsLock.withLock {
            if (chartOperations.containsKey(chartId)) {
                false // Operation already in progress
            } else {
                chartOperations[chartId] = operation
                true
            }
        }
    }

    // Emit an event to subscribers without suspending
    private fun emitEvent(event: DownloadEvent) {
        val success = _events.tryEmit(event)
        if (!success) {
            Log.w(TAG, "Failed to emit event: $event (buffer may be full)")
        }
    }

    // Update state efficiently with .update
    private fun updateState(chartId: String, state: DownloadState) {
        _downloadStates.update { currentStates ->
            currentStates.toMutableMap().apply {
                this[chartId] = state
            }
        }
    }

    /**
     * Downloads a chart
     */
    fun downloadChart(chart: Chart) {
        val chartId = chart.id

        viewModelScope.launch {
            try {
                // Check if operation is already in progress
                if (!setChartOperation(chartId, "download")) {
                    Log.w(TAG, "Download operation already in progress for chart: $chartId")
                    updateState(chartId, DownloadState.Error(
                        chartId,
                        context.getString(R.string.operation_in_progress),
                        ErrorType.DOWNLOAD_ERROR
                    ))
                    return@launch
                }

                // Update state immediately for UI feedback
                updateState(chartId, DownloadState.Downloading(chartId, 0f))

                // Validate chart data
                if (chart.bundleHash == null) {
                    throw IllegalArgumentException("Bundle hash is empty")
                }

                // Fetch the actual bundle download URL from the API
                val bundleResponse = apiClient.getChartBundleUrl(chart.id)
                val bundleUrl = bundleResponse.url

                // Start the download
                downloadServiceMonitor.startDownload(
                    id = chartId,
                    contentId = chart.id,
                    name = "${chart.track.title} - ${chart.track.artist}",
                    bundleUrl = bundleUrl,
                    isUpdate = chart.availableVersion != null
                )

                Log.d(TAG, "Download started for chart: $chartId")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to start download for chart: $chartId", e)

                // Clear operation and update state
                clearChartOperation(chartId)
                val errorMessage = when (e) {
                    is IllegalArgumentException -> e.message ?: "Invalid chart data"
                    else -> context.getString(R.string.failed_to_start_download)
                }

                updateState(chartId, DownloadState.Error(chartId, errorMessage, ErrorType.DOWNLOAD_ERROR))
                emitEvent(DownloadEvent.Error(chartId, errorMessage, ErrorType.DOWNLOAD_ERROR))
            }
        }
    }


    /**
     * Deletes a chart
     */
    fun deleteChart(
        chart: Chart,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val chartId = chart.id

        viewModelScope.launch {
            try {
                // Check if operation is already in progress
                if (!setChartOperation(chartId, "delete")) {
                    val errorMsg = context.getString(R.string.operation_in_progress)
                    onError(errorMsg)
                    return@launch
                }

                // Validate chart state
                val currentState = _downloadStates.value[chartId]
                if (currentState is DownloadState.Downloading || currentState is DownloadState.Extracting) {
                    clearChartOperation(chartId)
                    val errorMsg = context.getString(R.string.cannot_delete_during_download)
                    onError(errorMsg)
                    return@launch
                }

                // Remove files and persist deletion state from a single repository path.
                downloadRepository.deleteChart(chartId, chart.id)

                // Reset the state and clear operation
                updateState(chartId, DownloadState.Idle)
                clearChartOperation(chartId)

                Log.d(TAG, "Chart deleted successfully: $chartId")
                onSuccess()

            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete chart: $chartId", e)
                clearChartOperation(chartId)

                val errorMessage = when (e) {
                    is SecurityException -> context.getString(R.string.permission_denied_delete)
                    is IOException -> context.getString(R.string.storage_error_delete)
                    else -> context.getString(R.string.failed_to_delete_chart)
                }

                onError(errorMessage)
            }
        }
    }

    /**
     * Uninstalls a tour pass: deletes every associated chart folder, marks each
     * chart as not installed, and removes the tour pass from the root manifest.
     */
    fun uninstallTourPass(
        tourPass: TourPass,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                tourPass.charts.forEach { chart ->
                    downloadRepository.deleteChart(chart.id, chart.id)
                    updateState(chart.id, DownloadState.Idle)
                }
                tourPassStorageManager.removeInstalledTourPass(tourPass.id)
                emitEvent(DownloadEvent.Complete(tourPass.id))
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to uninstall tour pass: ${tourPass.id}", e)
                onError(context.getString(R.string.failed_to_delete_chart))
            }
        }
    }

    fun getDownloadState(chartId: String): StateFlow<DownloadState> {
        return downloadStates
            .map { it[chartId] ?: DownloadState.Idle }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = _downloadStates.value[chartId] ?: DownloadState.Idle
            )
    }

    /**
     * Seeds the download state of every chart of a tour pass so the aggregate
     * state reflects already installed charts without a new download. Charts
     * embedded in the tour pass payload carry no local install state, so the
     * locally persisted flag is read instead. All charts are resolved in a
     * single batched query so the installed status appears as fast as it does
     * on the chart details screen.
     */
    fun checkTourPassStatus(tourPass: TourPass) {
        viewModelScope.launch {
            val chartIds = tourPass.charts.map { it.id }
            val localCharts = chartLocalRepository.getItems(chartIds).first().getOrNull() ?: emptyList()
            val installedIds = localCharts
                .asSequence()
                .filter { it.isInstalled == true }
                .map { it.id }
                .toSet()

            tourPass.charts.forEach { chart ->
                val isInstalled = chart.id in installedIds || chart.isInstalled == true
                if (isInstalled && _downloadStates.value[chart.id] == null) {
                    updateState(chart.id, DownloadState.Installed(chart.id))
                }
            }
        }
    }

    /**
     * Downloads every chart of a tour pass sequentially, exposing the overall
     * progress through [getTourPassDownloadState]. Charts that are explicit
     * (when explicit content is disabled in settings) are skipped. A failure on
     * any chart does not abort the tour pass: the remaining charts are still
     * downloaded, but the tour pass is only marked as installed once every
     * available chart is installed.
     */
    fun downloadTourPass(tourPass: TourPass) {
        viewModelScope.launch {
            // Charts that should not be downloaded (e.g. explicit content when
            // disabled in settings) never count towards the tour pass progress.
            val charts = tourPass.charts.filter { isDownloadEligible(it) }
            if (charts.isEmpty()) return@launch

            var hasFailure = false
            try {
                charts.forEach { chart ->
                    // Charts that are already installed do not need to be downloaded again.
                    if (isChartInstalledLocally(chart)) return@forEach

                    // Reflect the ongoing operation immediately, before the
                    // first service event arrives, so the UI shows the loading
                    // state and the button disables against duplicate clicks.
                    updateState(chart.id, DownloadState.Downloading(chart.id, 0f))

                    try {
                        if (chart.bundleHash == null) {
                            throw IllegalArgumentException("Bundle hash is empty")
                        }

                        // Fetch the actual bundle download URL from the API.
                        val bundleResponse = apiClient.getChartBundleUrl(chart.id)

                        // Start the download through the foreground service.
                        downloadServiceMonitor.startDownload(
                            id = chart.id,
                            contentId = chart.id,
                            name = "${chart.track.title} - ${chart.track.artist}",
                            bundleUrl = bundleResponse.url,
                            isUpdate = false
                        )

                        // Wait for the chart to finish (or fail) before moving on.
                        val terminalState = waitForChartTerminalState(chart.id)
                        if (terminalState !is DownloadState.Installed) {
                            hasFailure = true
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to download chart ${chart.id} for tour pass ${tourPass.id}", e)
                        val errorMessage = context.getString(R.string.failed_to_start_download)
                        updateState(
                            chart.id,
                            DownloadState.Error(chart.id, errorMessage, ErrorType.DOWNLOAD_ERROR)
                        )
                        hasFailure = true
                    }
                }

                if (hasFailure) {
                    // Not every chart was installed: keep the tour pass
                    // uninstalled and ask the user to retry the remaining charts.
                    emitEvent(
                        DownloadEvent.Error(
                            tourPass.id,
                            context.getString(R.string.download_failed),
                            ErrorType.DOWNLOAD_ERROR
                        )
                    )
                    return@launch
                }

                // Mark the tour pass as installed so it shows up in the updates page.
                tourPassManager.markInstalled(tourPass.id)

                // Record the tour pass in the root manifest (survives app
                // uninstalls) and keep the local database in sync.
                tourPassStorageManager.addInstalledTourPass(tourPass)

                emitEvent(DownloadEvent.Complete(tourPass.id))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download tour pass: ${tourPass.id}", e)
                val errorMessage = context.getString(R.string.failed_to_start_download)
                emitEvent(DownloadEvent.Error(tourPass.id, errorMessage, ErrorType.DOWNLOAD_ERROR))
            }
        }
    }

    /**
     * Aggregated download state for a whole tour pass, derived from the
     * per-chart download states. Progress is the average of every chart's
     * progress (installed charts count as fully done). Charts that should not
     * be downloaded (e.g. explicit content when disabled in settings) are
     * excluded from the totals, so the tour pass is only considered installed
     * once all of its available charts are installed.
     */
    fun getTourPassDownloadState(tourPass: TourPass): StateFlow<DownloadState> {
        return _downloadStates
            .map { states -> aggregateTourPassDownloadState(tourPass, states) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = aggregateTourPassDownloadState(tourPass, _downloadStates.value)
            )
    }

    private suspend fun waitForChartTerminalState(chartId: String): DownloadState {
        return _downloadStates
            .map { it[chartId] }
            .filterNotNull()
            .first { state -> state is DownloadState.Installed || state is DownloadState.Error }
    }

    private fun isDownloadEligible(chart: Chart): Boolean =
        !chart.isExplicit || allowExplicitContent

    private suspend fun isChartInstalledLocally(chart: Chart): Boolean {
        val local = chartLocalRepository.getItem(chart.id).first().getOrNull()
        return local?.isInstalled == true || chart.isInstalled == true
    }

    private fun aggregateTourPassDownloadState(
        tourPass: TourPass,
        states: Map<String, DownloadState>
    ): DownloadState {
        val charts = tourPass.charts.filter { isDownloadEligible(it) }
        if (charts.isEmpty()) return DownloadState.Idle

        val total = charts.size
        val step = 1f / total
        var progress = 0f
        var installedCount = 0
        var hasActiveDownload = false
        var error: DownloadState.Error? = null

        for (chart in charts) {
            val state = states[chart.id]
            if (state == null) {
                if (chart.isInstalled == true) {
                    progress += step
                    installedCount++
                }
                continue
            }
            when (state) {
                is DownloadState.Installed -> {
                    progress += step
                    installedCount++
                }

                is DownloadState.Downloading -> {
                    hasActiveDownload = true
                    progress += step * state.progress.coerceIn(0f, 1f)
                }

                is DownloadState.Extracting -> {
                    hasActiveDownload = true
                    progress += step * state.progress.coerceIn(0f, 1f)
                }

                is DownloadState.Error -> error = state
                is DownloadState.Idle -> {}
            }
        }

        return when {
            installedCount == total -> DownloadState.Installed(tourPass.id)
            hasActiveDownload -> DownloadState.Downloading(
                tourPass.id,
                progress.coerceIn(0f, 1f),
                installedCount,
                total
            )
            error != null -> DownloadState.Error(tourPass.id, error.message, error.type)
            else -> DownloadState.Idle
        }
    }
}