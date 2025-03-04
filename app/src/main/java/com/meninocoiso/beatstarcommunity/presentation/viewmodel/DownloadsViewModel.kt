package com.meninocoiso.beatstarcommunity.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.beatstarcommunity.data.repository.ChartRepository
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

sealed class DownloadsState {
    data object Loading : DownloadsState()
    data class Success(val charts: List<Chart>) : DownloadsState()
    data class Error(val message: String?) : DownloadsState()
}

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    @Named("Local") private val chartRepository: ChartRepository
) : ViewModel() {
    private val _downloadsState = MutableStateFlow<DownloadsState>(DownloadsState.Loading)
    val downloadsState: StateFlow<DownloadsState> = _downloadsState.asStateFlow()

    init {
        fetchLocalCharts()
    }

    fun fetchLocalCharts() {
        _downloadsState.value = DownloadsState.Loading
        viewModelScope.launch {
            chartRepository.getCharts().collect { result ->
                _downloadsState.value = result.fold(
                    onSuccess = { charts -> DownloadsState.Success(charts) },
                    onFailure = { error -> DownloadsState.Error(error.message) }
                )
            }
        }
    }
}