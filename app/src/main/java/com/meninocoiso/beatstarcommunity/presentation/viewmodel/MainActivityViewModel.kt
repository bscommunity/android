package com.meninocoiso.beatstarcommunity.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.beatstarcommunity.BuildConfig
import com.meninocoiso.beatstarcommunity.data.repository.AppUpdateRepository
import com.meninocoiso.beatstarcommunity.data.repository.SettingsRepository
import com.meninocoiso.beatstarcommunity.domain.model.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface MainActivityUiState {
	data object Loading : MainActivityUiState
	data class Success(val settings: Settings) : MainActivityUiState
}

@HiltViewModel
class MainActivityViewModel @Inject constructor(
	@ApplicationContext private val context: Context,
	private val appUpdateRepository: AppUpdateRepository,
	private val settingsRepository: SettingsRepository,
) : ViewModel() {
	val uiState: StateFlow<MainActivityUiState> = settingsRepository.settingsFlow.map {
		MainActivityUiState.Success(it)
	}.stateIn(
		scope = viewModelScope,
		initialValue = MainActivityUiState.Loading,
		started = SharingStarted.WhileSubscribed(5_000),
	)

	init {
		viewModelScope.launch {
			appUpdateRepository.fetchLatestVersion()
				.catch { 
					// Log.d("MainActivityViewModel", "Error fetching version: $it")
					settingsRepository.setLatestVersion("")
				}
				.collect { fetchedVersion ->
					// Log.d("MainActivityViewModel", "Fetched version: $fetchedVersion")
					
					// Store the version in DataStore
					settingsRepository.setLatestVersion(fetchedVersion)
				}
		}
	}

	 fun cleanupOldUpdates() {
		val currentVersionCode = BuildConfig.VERSION_CODE

		viewModelScope.launch(Dispatchers.IO) {
			val lastCleanedVersion = settingsRepository.getLatestCleanedVersion()

			// Only clean if we've updated since last cleaning
			if (currentVersionCode > lastCleanedVersion) {
				viewModelScope.launch(Dispatchers.IO) {
					context.cacheDir.listFiles()?.forEach { file ->
						if (file.name.startsWith("update-") && file.extension == "apk") {
							Log.d("MainActivityViewModel", "Deleting old update file: ${file.name}")
							file.delete()
						}
					}

					// Update the last cleaned version
					settingsRepository.setLatestCleanedVersion(currentVersionCode)
				}
			}
		}
	}
}