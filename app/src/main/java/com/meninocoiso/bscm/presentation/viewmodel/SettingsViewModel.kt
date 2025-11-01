package com.meninocoiso.bscm.presentation.viewmodel

import android.content.Context
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
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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

sealed class AppUpdateState {
    data object Idle : AppUpdateState()
    data object UpToDate : AppUpdateState()
    data object Checking : AppUpdateState()
    data class Downloading(val progress: Float) : AppUpdateState()
    data class UpdateAvailable(val version: String) : AppUpdateState()
    data class ReadyToInstall(val apkFile: File) : AppUpdateState()
    data class Error(val message: String) : AppUpdateState()
}

private const val TAG = "SettingsViewModel"

/**
 * ViewModel for managing application settings
 * Handles cacheState management and updates for settings
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val appUpdateRepository: AppUpdateRepository,
    private val apiClient: ApiClient,
    private val cacheRepository: CacheRepository, // Inject CacheRepository
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
    private val _updateEvents = MutableSharedFlow<String>()
    val updateEvents: SharedFlow<String> = _updateEvents

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
                    _updateEvents.emit("Update available: ${newState.version}")
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

    /**
     * Check for app updates
     */
    fun checkAppUpdates() {
        _updateState.value = AppUpdateState.Checking

        val currentTime = System.currentTimeMillis()
        
        viewModelScope.launch {
            // First try to get cached version
            val cachedVersion = appUpdateRepository.getLatestVersion()
            
            Log.d(TAG, "Cached version: $cachedVersion")
            Log.d(TAG, "Current time: $currentTime")
            Log.d(TAG, "Last cache time: ${lastCacheTime ?: 0}")
            Log.d(TAG, "Time since last cache: ${currentTime - (lastCacheTime ?: 0)}")
            
            // If cached version is available and within the cache window, use it
            if (cachedVersion != "" && currentTime - (lastCacheTime ?: 0) < cacheWindowMs) {
                Log.d(TAG, "Using cached version: $cachedVersion")
                
                _updateState.value = appUpdateRepository.getUpdateState(cachedVersion)

                return@launch
            }

            lastCacheTime = currentTime

            // If no cached version, fetch from remote
            appUpdateRepository.fetchLatestVersion()
                .catch { exception ->
                    _updateState.value =
                        AppUpdateState.Error(exception.message ?: context.getString(R.string.failed_to_check_for_updates))
                }
                .collect { fetchedVersion ->
                    Log.d(TAG, "Fetched version: $fetchedVersion")
                    
                    // Store the version in DataStore
                    appUpdateRepository.setLatestVersion(fetchedVersion)

                    _updateState.value = appUpdateRepository.getUpdateState(fetchedVersion)
                }
        }
    }

    /** Helper to extract shrunk version name (removes suffix after last '-') */
    fun shrunkVersion(version: String): String = version.substringBeforeLast("-")

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
                _updateState.value = AppUpdateState.Error(e.message ?: context.getString(R.string.unknown_error))
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
                _updateState.value = AppUpdateState.Error(e.message ?: context.getString(R.string.unknown_error))
            }
        }
    }
}