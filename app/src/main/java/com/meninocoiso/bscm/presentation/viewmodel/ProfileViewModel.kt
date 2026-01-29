package com.meninocoiso.bscm.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.meninocoiso.bscm.domain.repository.ChartRepository
import com.meninocoiso.bscm.domain.result.ContentState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Named

private const val TAG = "ProfileViewModel"

/**
 * ViewModel for managing application settings
 * Handles cacheState management and updates for settings
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    @param:Named("Remote") private val remoteChartRepository: ChartRepository,
    @param:Named("Local") private val localChartRepository: ChartRepository,
) : ViewModel() {
    private val _profileState = MutableStateFlow<ContentState>(ContentState.Loading)
    val profileState: SharedFlow<ContentState> = _profileState.asStateFlow()


}