package com.meninocoiso.beatstarcommunity.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.beatstarcommunity.BuildConfig
import com.meninocoiso.beatstarcommunity.data.repository.AppUpdateRepository
import com.meninocoiso.beatstarcommunity.data.repository.SettingsRepository
import com.meninocoiso.beatstarcommunity.domain.enums.ThemePreference
import com.meninocoiso.beatstarcommunity.domain.model.Settings
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

	private val currentVersion = BuildConfig.VERSION_CODE.toString()

	init {
		checkAppUpdates(silentMode = true)
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

	/**
	 * Check for app updates
	 * @param silentMode If true, errors won't be exposed through the UI cacheState
	 */
	fun checkAppUpdates(silentMode: Boolean = false) {
		_updateState.value = AppUpdateState.Checking

		viewModelScope.launch {
			// First try to get cached version
			val cachedVersion = settingsRepository.getLatestVersion()

			if (cachedVersion != null) {
				compareVersionAndUpdateState(cachedVersion)
				return@launch
			}

			// If no cached version, fetch from remote
			appUpdateRepository.fetchLatestVersion()
				.catch { exception ->
					_updateState.value = AppUpdateState.Error(exception.message ?: "Failed to check for updates")
				}
				.collect { fetchedVersion ->
					// Store the version in DataStore
					settingsRepository.setLatestVersion(fetchedVersion)

					compareVersionAndUpdateState(fetchedVersion)
				}
		}
	}

	/**
	 * Helper function to compare versions and update cacheState
	 */
	private fun compareVersionAndUpdateState(fetchedVersion: String) {
		_updateState.value = if (fetchedVersion > currentVersion) {
			val apkFile = appUpdateRepository.getApkFile(fetchedVersion)

			if (apkFile != null) {
				AppUpdateState.ReadyToInstall(apkFile)
			} else {
				AppUpdateState.UpdateAvailable(fetchedVersion)
			}
		} else {
			AppUpdateState.UpToDate
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
		
		try {
			appUpdateRepository.installApk(apkFile)
		} catch (e: Exception) {
			Log.e(TAG, "APK installation failed", e)
			_updateState.value = AppUpdateState.Error(e.message ?: "Unknown error")
		}
	}
}