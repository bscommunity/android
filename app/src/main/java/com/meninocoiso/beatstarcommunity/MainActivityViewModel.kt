package com.meninocoiso.beatstarcommunity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.beatstarcommunity.data.repository.SettingsRepository
import com.meninocoiso.beatstarcommunity.domain.model.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Data class that represents the state of activity.
 */
sealed interface MainActivityUiState {
	data object Loading : MainActivityUiState
	data class Success(
		val settings: Settings
	) : MainActivityUiState
}

@HiltViewModel
class MainActivityViewModel @Inject constructor(
	settingsRepository: SettingsRepository,
) : ViewModel() {
	val uiState: StateFlow<MainActivityUiState> = settingsRepository.settingsFlow.map {
		MainActivityUiState.Success(it)
	}.stateIn(
		scope = viewModelScope,
		initialValue = MainActivityUiState.Loading,
		started = SharingStarted.WhileSubscribed(5_000),
	)
}