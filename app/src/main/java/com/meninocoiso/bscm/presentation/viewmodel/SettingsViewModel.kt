package com.meninocoiso.bscm.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.data.repository.AppUpdateRepository
import com.meninocoiso.bscm.data.repository.CacheRepository
import com.meninocoiso.bscm.data.repository.SettingsRepository
import com.meninocoiso.bscm.domain.enums.ThemePreference
import com.meninocoiso.bscm.domain.model.internal.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val settingsRepository: SettingsRepository,
    private val cacheRepository: CacheRepository,
    private val appUpdateRepository: AppUpdateRepository
) : ViewModel() {
    /**
     * Expose settings as a StateFlow for reactive UI updates
     */
    val uiState: StateFlow<Settings> = settingsRepository.settingsFlow
        .map { it }  // Simplified - removed unnecessary mapping if Settings object structure matches
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = Settings()
        )

    private val _updateState = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
    val updateState: StateFlow<AppUpdateState> = _updateState.asStateFlow()

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
                        AppUpdateState.Error(exception.message ?: "Failed to check for updates")
                }
                .collect { fetchedVersion ->
                    Log.d(TAG, "Fetched version: $fetchedVersion")
                    
                    // Store the version in DataStore
                    appUpdateRepository.setLatestVersion(fetchedVersion)

                    _updateState.value = appUpdateRepository.getUpdateState(fetchedVersion)
                }
        }
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
                _updateState.value = AppUpdateState.Error(e.message ?: "Unknown error")
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
                _updateState.value = AppUpdateState.Error(e.message ?: "Unknown error")
            }
        }
    }
}