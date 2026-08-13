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
import com.meninocoiso.bscm.di.ApplicationScope
import com.meninocoiso.bscm.domain.enums.ErrorType
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.domain.model.internal.Settings
import com.meninocoiso.bscm.domain.repository.ChartLocalRepository
import com.meninocoiso.bscm.domain.state.DownloadState
import com.meninocoiso.bscm.monitor.DownloadServiceMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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
    @param:ApplicationScope private val applicationScope: CoroutineScope,
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
     * database reads. The store is shared across screens, so when a chart is
     * deleted anywhere (e.g. from its chart details screen) the flow re-emits
     * without it and stale states are cleared, keeping every details screen in
     * sync without having to leave and re-enter.
     */
    private fun observeInstalledCharts() {        viewModelScope.launch {
            var previouslyInstalled = emptySet<String>()
            chartManager.installedCharts.collect { charts ->
                val currentlyInstalled = charts.mapNotNullTo(mutableSetOf()) {
                    if (it.isInstalled == true) it.id else null
                }

                // Seed charts that just became installed.
                (currentlyInstalled - previouslyInstalled).forEach { id ->
                    if (_downloadStates.value[id] == null) {
                        updateState(id, DownloadState.Installed(id))
                    }
                }

                // Clear charts that were uninstalled so screens update reactively.
                (previouslyInstalled - currentlyInstalled).forEach { id ->
                    if (_downloadStates.value[id] is DownloadState.Installed) {
                        updateState(id, DownloadState.Idle)
                    }
                }

                previouslyInstalled = currentlyInstalled
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
     * Checks the installation status of a chart. The install flag is resolved
     * from the in-memory store when the chart is known there (feed charts,
     * scanned/downloaded charts), so the status appears on the first frame even
     * when the chart passed by the navigator (e.g. from a tour pass) carries no
     * install state and no database read is needed.
     */
    fun checkStatus(chart: Chart) {
        viewModelScope.launch {
            try {
                val storeChart = chartManager.getChartFromStore(chart.id)
                val isInstalled = storeChart?.isInstalled == true || chart.isInstalled == true
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
                    // Merge whatever the in-memory store already knows about the
                    // chart, so the merged state (install/like/bookmark) is
                    // correct on the very first frame, before the Room read
                    // arrives. For feed charts the store already carries the
                    // merged local state; for tour pass charts the Room read
                    // refines it moments later.
                    initialValue = mergeStoredState(chart)
                )
        }

/**
     * Merges the locally persisted interaction state (like/bookmark/install)
     * held by the in-memory store into a chart object, without any I/O.
     */
    private fun mergeStoredState(chart: Chart): Chart {
        val stored = chartManager.getChartFromStore(chart.id) ?: return chart
        return chart.copy(
            likedAt = stored.likedAt ?: chart.likedAt,
            bookmarkedAt = stored.bookmarkedAt ?: chart.bookmarkedAt,
            isInstalled = if (stored.isInstalled == true) true else chart.isInstalled,
        )
    }

    /**
     * Live version of a [TourPass] shown on the details screen. The payload
     * passed by the navigator may carry a stale install flag; the manager's
     * installed-ids signal is merged in, so the download/uninstall action
     * reflects live state from the very first frame and self-heals as soon as
     * the manifest seed or an optimistic update lands.
     *
     * Cached per id for the same reason as [observeChartState]: recreating the
     * flow on every recomposition would start a new one, and the payload's
     * install flag would flicker back before the merge re-emits.
     */
    private val tourPassMergedCache = object :
        LinkedHashMap<String, StateFlow<TourPass>>(16, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, StateFlow<TourPass>>
        ) = size > 10
    }

    fun observeTourPassState(tourPass: TourPass): StateFlow<TourPass> =
        tourPassMergedCache.getOrPut(tourPass.id) {
            tourPassManager.installedTourPassIds
                .map { installedIds ->
                    // The manager's seeded signal is the single source of truth;
                    // the payload's own flag only bridges the window before the
                    // manifest seed lands. Deriving the flag this way lets the
                    // merged state downgrade right after an uninstall instead of
                    // pinning the stale payload flag forever.
                    tourPass.copy(
                        isInstalled = tourPass.id in installedIds ||
                            (tourPass.isInstalled == true &&
                                !tourPassManager.hasSeededInstalledIds())
                    )
                }
                .distinctUntilChanged()
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.Eagerly,
                    initialValue = tourPass.copy(
                        isInstalled = tourPass.isInstalled == true ||
                            tourPass.id in tourPassManager.installedTourPassIds.value
                    )
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
     * Deletes a chart.
     *
     * Runs in the application scope: the user may leave the details screen
     * (destroying this ViewModel and cancelling its scope) while the folder
     * deletion is in flight, and the operation must still complete to keep
     * the file system, store and database consistent.
     */
    fun deleteChart(
        chart: Chart,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val chartId = chart.id

        applicationScope.launch {
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
                downloadRepository.deleteChart(chartId)

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
     *
     * Runs in the application scope so the deletion completes even if the user
     * leaves the details screen mid-operation. Every chart's operation lock is
     * acquired before any file is touched, so an in-flight download cannot race
     * the deletion; if any chart is busy the whole uninstall is aborted.
     */
    fun uninstallTourPass(
        tourPass: TourPass,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        applicationScope.launch {
            val acquiredLocks = mutableListOf<String>()
            try {
                tourPass.charts.forEach { chart ->
                    if (!setChartOperation(chart.id, "delete")) {
                        val errorMsg = context.getString(R.string.operation_in_progress)
                        onError(errorMsg)
                        return@launch
                    }
                    acquiredLocks += chart.id
                }

                tourPass.charts.forEach { chart ->
                    downloadRepository.deleteChart(chart.id)
                    updateState(chart.id, DownloadState.Idle)
                }
                tourPassStorageManager.removeInstalledTourPass(tourPass.id)
                tourPassManager.markUninstalled(tourPass.id)
                emitEvent(DownloadEvent.Complete(tourPass.id))
                onSuccess()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to uninstall tour pass: ${tourPass.id}", e)
                onError(context.getString(R.string.failed_to_delete_chart))
            } finally {
                acquiredLocks.forEach { clearChartOperation(it) }
            }
        }
    }

    fun getDownloadState(chartId: String): StateFlow<DownloadState> {
        return downloadStates
            .map { it[chartId] ?: DownloadState.Idle }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                // The in-memory store resolves the install state on the very
                // first frame, before any seeding/database read lands.
                initialValue = _downloadStates.value[chartId] ?: storeInstallState(chartId)
            )
    }

    /**
     * Resolves the install state of a chart from the in-memory store, without
     * any I/O.
     */
    private fun storeInstallState(chartId: String): DownloadState =
        if (chartManager.getChartFromStore(chartId)?.isInstalled == true) {
            DownloadState.Installed(chartId)
        } else {
            DownloadState.Idle
        }

    /**
     * Seeds the download state of every chart of a tour pass so the aggregate
     * state reflects already installed charts without a new download. Charts
     * embedded in the tour pass payload carry no local install state, so the
     * locally persisted flag is read instead. All charts are resolved in a
     * single batched query so the installed status appears as fast as it does
     * on the chart details screen.
     *
     * The persisted "download tour pass" flag is only ever walked back when
     * every source positively confirms every chart of the tour pass was
     * deleted; a failed or empty read alone is never treated as evidence, so
     * the flag seeded for the first frame survives this background pass.
     */
    fun checkTourPassStatus(tourPass: TourPass) {
        viewModelScope.launch {
            // Resolve the installed status from in-memory sources first (no
            // I/O): the tour pass's own flag and the shared chart store. This
            // makes the aggregate correct on the very first frame, exactly
            // like getDownloadState resolves chart install state for the chart
            // details screen, instead of waiting for the reads below.
            if (tourPass.isInstalled == true && tourPass.id !in tourPassManager.installedTourPassIds.value) {
                tourPassManager.markInstalled(tourPass.id)
            }
            tourPass.charts.forEach { chart ->
                val isInstalled = chartManager.getChartFromStore(chart.id)?.isInstalled == true ||
                    chart.isInstalled == true
                if (isInstalled && _downloadStates.value[chart.id] == null) {
                    updateState(chart.id, DownloadState.Installed(chart.id))
                }
            }

            // Per-chart Room read: seeds individual track tiles correctly even
            // for charts downloaded standalone, independent of the tour pass's
            // own install flag below. A null result means the read failed, not
            // that nothing is installed, so it is kept distinct here.
            val chartIds = tourPass.charts.map { it.id }
            val localCharts = chartLocalRepository.getItems(chartIds).first().getOrNull()

            val locallyInstalledIds = localCharts
                ?.asSequence()
                ?.filter { it.isInstalled == true }
                ?.map { it.id }
                ?.toSet()
                ?: emptySet()

            tourPass.charts.forEach { chart ->
                if (chart.id in locallyInstalledIds && _downloadStates.value[chart.id] == null) {
                    updateState(chart.id, DownloadState.Installed(chart.id))
                }
            }

            // The installed flag lives in the manager (manifest-seeded at
            // construction and updated optimistically on install), so there is
            // nothing to seed here beyond bridging a payload that already says
            // installed. Only the downgrade below is reconciled in the
            // background, and never on the hot path that renders the button.
            val persistedAsInstalled = tourPass.isInstalled == true ||
                tourPass.id in tourPassManager.installedTourPassIds.value
            if (!persistedAsInstalled) return@launch // Never installed — nothing to reconcile.

            // The only reason to walk that back is the rare, deliberate case
            // where the user removed every chart individually. Only do it on
            // positive confirmation across every source — including the
            // ViewModel's own live state, the freshest one — never just because
            // one particular read came back empty.
            if (localCharts == null) return@launch // Read failed: not enough signal to downgrade.

            val anyChartInstalled = tourPass.charts.any { chart ->
                chart.id in locallyInstalledIds ||
                    chart.isInstalled == true ||
                    _downloadStates.value[chart.id] is DownloadState.Installed ||
                    chartManager.getChartFromStore(chart.id)?.isInstalled == true
            }

            if (!anyChartInstalled) {
                tourPassManager.markUninstalled(tourPass.id)
            }
        }
    }

    /**
     * Downloads every chart of a tour pass sequentially, exposing the overall
     * progress through [getTourPassDownloadState]. Charts that are explicit
     * (when explicit content is disabled in settings) are skipped. A failure on
     * any chart does not abort the tour pass: the remaining charts are still
     * downloaded. The tour pass is considered installed as long as at least one
     * of its available charts is installed.
     */
    fun downloadTourPass(tourPass: TourPass) {
        viewModelScope.launch {
            // Charts that should not be downloaded (e.g. explicit content when
            // disabled in settings) never count towards the tour pass progress.
            val charts = tourPass.charts.filter { isDownloadEligible(it) }
            if (charts.isEmpty()) return@launch

            var hasFailure = false
            var installedAny = false
            try {
                charts.forEach { chart ->
                    // Charts that are already installed do not need to be downloaded again.
                    if (isChartInstalledLocally(chart)) {
                        installedAny = true
                        return@forEach
                    }

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
                            name = "${chart.track.title} - ${chart.track.artist}",
                            bundleUrl = bundleResponse.url,
                            isUpdate = false
                        )

                        // Wait for the chart to finish (or fail) before moving on.
                        val terminalState = waitForChartTerminalState(chart.id)
                        if (terminalState is DownloadState.Installed) {
                            installedAny = true
                        } else {
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
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download tour pass: ${tourPass.id}", e)
                hasFailure = true
            }

            // The tour pass stays installed as long as at least one of its
            // available charts remains installed.
            if (installedAny) {
                // Update the live installed-ids signal before persisting: the
                // aggregate flips to Installed as soon as the last chart's
                // terminal state lands, so seeding the flag only after the
                // manifest write would briefly re-show the download button
                // (Idle flash) while that I/O is in flight.
                tourPassManager.markInstalled(tourPass.id)
                // Persist in the application scope: the user may leave the
                // screen (destroying this ViewModel and cancelling its scope)
                // right after the in-memory signal lands, and the manifest
                // write must still complete for the installed state to
                // survive restarts.
                applicationScope.launch {
                    try {
                        // Record the tour pass in the root manifest (survives
                        // app uninstalls) and keep the local database in sync.
                        tourPassStorageManager.addInstalledTourPass(tourPass)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to persist installed tour pass: ${tourPass.id}", e)
                    }
                }
            }

            if (hasFailure) {
                // Not every chart was installed: the user can retry the
                // remaining charts from their individual details screens.
                emitEvent(
                    DownloadEvent.Error(
                        tourPass.id,
                        context.getString(R.string.download_failed),
                        ErrorType.DOWNLOAD_ERROR
                    )
                )
            } else {
                emitEvent(DownloadEvent.Complete(tourPass.id))
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
     *
     * The tour pass reaches the installed state as soon as the user presses the
     * "download tour pass" button (persisted flag in the local database and
     * root manifest), never just because some of its charts were installed
     * individually. It stays installed until the user uninstalls the tour pass;
     * a manual deletion of every available chart is reconciled asynchronously
     * by the status check instead of being required on the first frame.
     *
     * The returned flow is cached per tour pass id. Recreating it on every
     * recomposition would start a new Lazily-started flow that keeps collecting
     * in [viewModelScope] for the ViewModel's lifetime; the cache keeps
     * repeated calls (including the un-remembered call site on the details
     * screen) on a single shared flow. The cached flow closes over the first
     * [TourPass] passed to it, which is fine because a tour pass's chart list
     * is stable.
     */
    // Cache of the flows returned by [getTourPassDownloadState] (see above).
    private val tourPassStateCache = object :
        LinkedHashMap<String, StateFlow<DownloadState>>(16, 0.75f, true) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, StateFlow<DownloadState>>
        ) = size > 10
    }

    fun getTourPassDownloadState(tourPass: TourPass): StateFlow<DownloadState> =
        tourPassStateCache.getOrPut(tourPass.id) {
            combine(_downloadStates, tourPassManager.installedTourPassIds) { states, installedIds ->
                aggregateTourPassDownloadState(
                    tourPass,
                    states,
                    isTourPassInstalled(tourPass, installedIds)
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                // The manager's manifest seed can land a few frames later, so
                // the payload flag and the in-memory store bridge the very
                // first frame.
                initialValue = aggregateTourPassDownloadState(
                    tourPass,
                    _downloadStates.value,
                    isTourPassInstalled(tourPass, tourPassManager.installedTourPassIds.value)
                )
            )
        }

    /**
     * Resolves the installed flag of a tour pass from in-memory sources only,
     * without any I/O: the manager's live installed-ids signal (manifest-seeded
     * + optimistic), the shared in-memory cache, and — only until the manifest
     * seed lands — the tour pass's own payload flag. Mirrors
     * [storeInstallState] for charts, so the tour pass shows as installed on
     * the very first frame instead of after a database or manifest read. Once
     * the seed lands, the payload flag is ignored so an uninstall drops the
     * state to Idle immediately.
     */
    private fun isTourPassInstalled(tourPass: TourPass, installedIds: Set<String>): Boolean =
        tourPass.id in installedIds ||
            (tourPass.isInstalled == true && !tourPassManager.hasSeededInstalledIds()) ||
            tourPassManager.getTourPassFromStore(tourPass.id)?.isInstalled == true

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
        states: Map<String, DownloadState>,
        isTourPassInstalled: Boolean
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
                // Charts may not be seeded in the state map yet on the first
                // frame; the in-memory store resolves install state without
                // any database read, so the aggregate is correct immediately.
                val storeChart = chartManager.getChartFromStore(chart.id)
                if (storeChart?.isInstalled == true || chart.isInstalled == true) {
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
            hasActiveDownload -> DownloadState.Downloading(
                tourPass.id,
                progress.coerceIn(0f, 1f),
                installedCount,
                total
            )
            // The tour pass counts as installed as soon as the persisted flag
            // is set (the "download tour pass" button was pressed), resolved
            // synchronously on the first frame. A manual deletion of every
            // chart is reconciled asynchronously by the status check, not on
            // the hot path that renders the button.
            isTourPassInstalled -> DownloadState.Installed(tourPass.id)
            error != null -> DownloadState.Error(tourPass.id, error.message, error.type)
            else -> DownloadState.Idle
        }
    }
}