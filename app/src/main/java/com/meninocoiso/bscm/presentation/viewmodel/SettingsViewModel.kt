package com.meninocoiso.bscm.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.remote.ApiClient
import com.meninocoiso.bscm.data.repository.AppUpdateRepository
import com.meninocoiso.bscm.data.repository.CacheRepository
import com.meninocoiso.bscm.data.repository.SettingsRepository
import com.meninocoiso.bscm.domain.enums.ThemePreference
import com.meninocoiso.bscm.domain.model.internal.ContributionCategory
import com.meninocoiso.bscm.domain.model.internal.Settings
import com.meninocoiso.bscm.domain.result.UiText
import com.meninocoiso.bscm.domain.state.AppUpdateState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

private const val TAG = "SettingsViewModel"

/**
 * ViewModel for managing application settings
 * Handles cacheState management and updates for settings
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val apiClient: ApiClient,
    private val settingsRepository: SettingsRepository,
    private val appUpdateRepository: AppUpdateRepository,
    private val cacheRepository: CacheRepository,
) : ViewModel() {
    /**
     * Expose settings as a StateFlow for reactive UI updates
     */
    val uiState: StateFlow<Settings> = settingsRepository.settingsFlow
        .map { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Settings()
        )

    private val _updateState = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
    val updateState: StateFlow<AppUpdateState> = _updateState.asStateFlow()

    // New: update events for one-off notifications (snackbar, etc.)
    private val _updateEvents = MutableSharedFlow<UiText>()
    val updateEvents: SharedFlow<UiText> = _updateEvents

    private var lastUpdateEvent: UiText? = null
    private var lastUpdateEventAt = 0L
    private val updateEventDedupWindowMs = 1_500L
    private val minCheckingVisibleMs = 350L

    // Contributors state (not persisted)
    data class ContributorsState(
        val isLoading: Boolean = false,
        val items: List<ContributionCategory> = emptyList()
    )
    private val _contributorsState = MutableStateFlow(ContributorsState())
    val contributorsState: StateFlow<ContributorsState> = _contributorsState.asStateFlow()

    init {
        // Initialize the update state with the current version
        viewModelScope.launch {
            val cachedVersion = appUpdateRepository.getLatestVersion()
            if (cachedVersion != "") {
                val newState = appUpdateRepository.getUpdateState(cachedVersion)
                
                // We verify the new state to not trigger the snackbar if the app is up to date
                _updateState.value = if (newState is AppUpdateState.UpToDate) {
                    AppUpdateState.Idle
                } else {
                    newState
                }
                // Emit event if update is available
                if (newState is AppUpdateState.UpdateAvailable) {
                    emitUpdateEvent(UiText.Res(R.string.update_available, newState.version))
                }
            }
        }
    }

    /**
     * Trigger loading contributors only once (first open)
     */
    fun loadContributorsIfNeeded() {
        val current = _contributorsState.value
        if (current.isLoading || current.items.isNotEmpty()) return

        viewModelScope.launch {
            _contributorsState.value = current.copy(isLoading = true)

            // Load from CacheRepository first
            val cached = cacheRepository.getContributors()
            if (cached.isNotEmpty()) {
                _contributorsState.value = ContributorsState(isLoading = false, items = cached)
                return@launch
            }

            val result = apiClient.getContributors()
            if (result.isNotEmpty()) {
                _contributorsState.value = ContributorsState(isLoading = false, items = result)
                cacheRepository.setContributors(result)
            } else {
                // Try again one more time if the result is empty
                try {
                    val retryResult = apiClient.getContributors()
                    _contributorsState.value = ContributorsState(isLoading = false, items = retryResult)
                    cacheRepository.setContributors(retryResult)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load contributors on retry", e)
                    _contributorsState.value = current.copy(isLoading = false)
                }
            }
        }
    }

    /**
     * Toggle explicit content setting
     */
    fun allowExplicitContent(allow: Boolean) {
        viewModelScope.launch {
            settingsRepository.setExplicitContent(allow)
        }
    }
    
    /*
    *  Toggle gameplay preview video setting
    */
    fun enableGameplayPreviewVideo(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.setGameplayPreviewVideo(enable)
        }
    }

    /**
     * Toggle dynamic colors setting
     */
    fun useDynamicColors(use: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColors(use)
        }
    }

    /**
     * Update app theme
     */
    fun updateAppTheme(theme: ThemePreference) {
        viewModelScope.launch {
            settingsRepository.setAppTheme(theme)
        }
    }

    var lastCacheTime: Long? = null
    val cacheWindowMs = 5_000L
    private var checkUpdatesJob: Job? = null
    private var lastUpdateRequestAt = 0L
    private val updateRequestGateMs = 15_000L

    /**
     * Check for app updates
     */
    fun checkAppUpdates() {
        if (checkUpdatesJob?.isActive == true) {
            return
        }

        checkUpdatesJob = viewModelScope.launch {
            _updateState.value = AppUpdateState.Checking

            val now = System.currentTimeMillis()
            val cachedVersion = appUpdateRepository.getLatestVersion()
            val hasCachedVersion = cachedVersion.isNotBlank()
            val hasFreshCache = hasCachedVersion && now - (lastCacheTime ?: 0) < cacheWindowMs
            val isRequestGateClosed = now - lastUpdateRequestAt < updateRequestGateMs

            // Single request-gate policy: use cache while the gate is closed.
            if (hasFreshCache || isRequestGateClosed) {
                if (hasCachedVersion) {
                    val resolvedState = appUpdateRepository.getUpdateState(cachedVersion)
                    showCheckResultWithDelay(resolvedState)
                } else {
                    delay(minCheckingVisibleMs)
                    _updateState.value = AppUpdateState.Idle
                }
                return@launch
            }

            lastUpdateRequestAt = now

            appUpdateRepository.fetchLatestVersion()
                .catch { exception ->
                    // Reopen the gate on failure so user can retry immediately.
                    lastUpdateRequestAt = 0L

                    val errorState = AppUpdateState.Error(
                        UiText.Res(
                            R.string.failed_to_check_for_updates,
                            exception.localizedMessage ?: ""
                        )
                    )
                    _updateState.value = errorState
                    emitCheckUpdateResultEvent(errorState)
                }
                .collect { fetchedVersion ->
                    appUpdateRepository.setLatestVersion(fetchedVersion)
                    lastCacheTime = System.currentTimeMillis()

                    val state = appUpdateRepository.getUpdateState(fetchedVersion)
                    _updateState.value = state
                    emitCheckUpdateResultEvent(state)
                }
        }
    }

    private suspend fun showCheckResultWithDelay(state: AppUpdateState) {
        delay(minCheckingVisibleMs)
        _updateState.value = state
        emitCheckUpdateResultEvent(state)
    }

    private suspend fun emitCheckUpdateResultEvent(state: AppUpdateState) {
        when (state) {
            is AppUpdateState.UpToDate -> emitUpdateEvent(UiText.Res(R.string.up_to_date))
            is AppUpdateState.UpdateAvailable -> emitUpdateEvent(UiText.Res(R.string.update_available, state.version))
            is AppUpdateState.ReadyToInstall -> emitUpdateEvent(UiText.Res(R.string.update_ready))
            is AppUpdateState.Error -> emitUpdateEvent(state.message)
            else -> Unit
        }
    }

    private suspend fun emitUpdateEvent(message: UiText) {
        val now = System.currentTimeMillis()
        val isDuplicateRecent = lastUpdateEvent == message && (now - lastUpdateEventAt) < updateEventDedupWindowMs
        if (isDuplicateRecent) return

        lastUpdateEvent = message
        lastUpdateEventAt = now
        _updateEvents.emit(message)
    }

    fun downloadUpdate(version: String) {
        Log.d(TAG, "Downloading update for version: $version")

        viewModelScope.launch {
            _updateState.value = AppUpdateState.Downloading(0f)

            try {
                val apkFile = appUpdateRepository.downloadApkUpdate(version) {
                    _updateState.value = it
                }

                _updateState.value = AppUpdateState.ReadyToInstall(apkFile)
            } catch (e: Exception) {
                Log.e(TAG, "APK download failed", e)
                _updateState.value = AppUpdateState.Error(UiText.Res(R.string.unknown_error))
            }
        }
    }

    fun installApk(apkFile: File) {
        Log.d(TAG, "Installing APK: ${apkFile.absolutePath}")

        viewModelScope.launch {
            try {
                appUpdateRepository.installApk(apkFile)
            } catch (e: Exception) {
                Log.e(TAG, "APK installation failed", e)
                _updateState.value = AppUpdateState.Error(UiText.Res(R.string.unknown_error))
            }
        }
    }
}