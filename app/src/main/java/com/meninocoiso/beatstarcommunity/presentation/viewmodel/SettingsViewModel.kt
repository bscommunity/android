package com.meninocoiso.beatstarcommunity.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.beatstarcommunity.data.repository.SettingsRepository
import com.meninocoiso.beatstarcommunity.domain.enums.ThemePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
	val allowExplicitContent: Boolean = false,
	val useDynamicColors: Boolean = true,
	val theme: ThemePreference = ThemePreference.SYSTEM
)

/**
 * Data class that represents the state of the view model.
 */
private data class SettingsViewModelState(
	val allowExplicitContent: Boolean = false,
	val useDynamicColors: Boolean = true,
	val themePreference: ThemePreference = ThemePreference.SYSTEM
) {
	fun asUiState() = SettingsUiState(
		allowExplicitContent = allowExplicitContent,
		useDynamicColors = useDynamicColors,
		theme = themePreference
	)
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
	private val settingsRepository: SettingsRepository
) : ViewModel() {
	private val viewModelState = MutableStateFlow(value = SettingsViewModelState())

	val uiState = viewModelState.map { it.asUiState() }.stateIn(
		scope = viewModelScope,
		started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
		initialValue = viewModelState.value.asUiState()
	)

	init {
		watchAppConfigurationStream()
	}

	private fun watchAppConfigurationStream() {
		viewModelScope.launch {
			settingsRepository.settingsFlow.collectLatest { settings ->
				viewModelState.update { state ->
					state.copy(
						allowExplicitContent = settings.allowExplicitContent,
						useDynamicColors = settings.useDynamicColors,
						themePreference = settings.theme
					)
				}

				println(viewModelState)
			}
		}
	}

	// Functions to update the user preferences
	fun allowExplicitContent(allow: Boolean) {
		viewModelScope.launch {
			settingsRepository.allowExplicitContent(allow)
		}
	}

	fun useDynamicColors(use: Boolean) {
		viewModelScope.launch {
			settingsRepository.useDynamicColors(use)
		}
	}

	fun updateAppTheme(theme: ThemePreference) {
		viewModelScope.launch {
			settingsRepository.updateAppTheme(theme)
		}
	}
}