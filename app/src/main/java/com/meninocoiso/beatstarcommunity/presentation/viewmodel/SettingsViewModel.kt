package com.meninocoiso.beatstarcommunity.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.beatstarcommunity.data.repository.SettingsRepository
import com.meninocoiso.beatstarcommunity.domain.enums.ThemePreference
import com.meninocoiso.beatstarcommunity.domain.model.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing application settings
 * Handles state management and updates for settings
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
	private val settingsRepository: SettingsRepository
) : ViewModel() {

	/**
	 * Expose settings as a StateFlow for reactive UI updates
	 */
	val uiState: StateFlow<Settings> = settingsRepository.settingsFlow
		.map { settings ->
			Settings(
				allowExplicitContent = settings.allowExplicitContent,
				useDynamicColors = settings.useDynamicColors,
				theme = settings.theme
			)
		}
		.stateIn(
			scope = viewModelScope,
			started = SharingStarted.WhileSubscribed(5_000),
			initialValue = Settings()
		)

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
	 * Set the folder uri
	 */
	fun setFolderUri(uri: String) {
		viewModelScope.launch {
			settingsRepository.setFolderUri(uri)
		}
	}

	/*
	* Get the current folder uri
	* */
	suspend fun getFolderUri(): String? {
		return settingsRepository.getFolderUri()
	}
}