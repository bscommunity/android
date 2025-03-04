package com.meninocoiso.beatstarcommunity.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.beatstarcommunity.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChartDetailsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
     /**
      * Sets the folder URI in the settings repository.
      *
      * @param uri The URI of the folder to be set.
      */
     fun setFolderUri(uri: String) {
         viewModelScope.launch {
             settingsRepository.setFolderUri(uri)
         }
     }

     /**
      * Retrieves the folder URI from the settings repository.
      *
      * @return The URI of the folder, or null if not set.
      */
     suspend fun getFolderUri(): String? {
         return settingsRepository.getFolderUri()
     }
}