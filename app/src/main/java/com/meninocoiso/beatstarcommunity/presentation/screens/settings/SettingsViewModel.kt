package com.meninocoiso.beatstarcommunity.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.beatstarcommunity.data.UserPreferencesRepository
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
	private val userPreferencesRepository: UserPreferencesRepository
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
			userPreferencesRepository.userPreferencesFlow.collectLatest { userPreferences ->
				viewModelState.update { state ->
					state.copy(
						allowExplicitContent = userPreferences.allowExplicitContent,
						useDynamicColors = userPreferences.useDynamicColors,
						themePreference = userPreferences.theme
					)
				}

				println(viewModelState)
			}
		}
	}

	fun allowExplicitContent(allow: Boolean) {
		viewModelScope.launch {
			userPreferencesRepository.allowExplicitContent(allow)
		}
	}

	fun useDynamicColors(use: Boolean) {
		viewModelScope.launch {
			userPreferencesRepository.useDynamicColors(use)
		}
	}

	fun updateAppTheme(theme: ThemePreference) {
		viewModelScope.launch {
			userPreferencesRepository.updateAppTheme(theme)
		}
	}
}