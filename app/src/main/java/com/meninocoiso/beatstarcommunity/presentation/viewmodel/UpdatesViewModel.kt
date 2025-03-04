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

sealed class UpdatesState {
    data object Loading : UpdatesState()
    data class Success(val charts: List<Chart>) : UpdatesState()
    data class Error(val message: String?) : UpdatesState()
}

@HiltViewModel
class UpdatesViewModel @Inject constructor(
    @Named("Local") private val chartRepository: ChartRepository
) : ViewModel() {
    private val _downloadsState = MutableStateFlow<UpdatesState>(UpdatesState.Loading)
    val downloadsState: StateFlow<UpdatesState> = _downloadsState.asStateFlow()

    init {
        fetchLocalCharts()
    }
    
    private fun fetchLocalCharts() {
        _downloadsState.value = UpdatesState.Loading
        viewModelScope.launch {
            chartRepository.getCharts().collect { result ->
                _downloadsState.value = result.fold(
                    onSuccess = { charts -> UpdatesState.Success(charts) },
                    onFailure = { error -> UpdatesState.Error(error.message) }
                )
            }
        }
    }
}