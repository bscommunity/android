package com.meninocoiso.bscm.presentation.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.manager.TourPassManager
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.domain.result.ContentResult
import com.meninocoiso.bscm.domain.result.UiText
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "TourPassDetailsViewModel"

@HiltViewModel
class TourPassDetailsViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val tourPassManager: TourPassManager
) : ViewModel() {
    private val _tourPass = MutableStateFlow<ContentResult<TourPass>>(ContentResult.Loading)
    val tourPass: StateFlow<ContentResult<TourPass>> = _tourPass.asStateFlow()

    fun fetchTourPassById(id: String?) {
        if (id.isNullOrEmpty()) {
            _tourPass.value = ContentResult.Error(UiText.Res(R.string.invalid_tour_pass_id))
            return
        }

        viewModelScope.launch {
            _tourPass.value = ContentResult.Loading

            try {
                tourPassManager.getTourPass(id).collect { result ->
                    when (result) {
                        is ContentResult.Success -> {
                            Log.d(TAG, "Tour pass data loaded successfully")
                            _tourPass.value = ContentResult.Success(result.data)
                        }

                        is ContentResult.Error -> {
                            val msg = result.message
                            Log.e(TAG, "Error fetching tour pass: $msg", result.cause)
                            _tourPass.value = ContentResult.Error(msg)
                        }

                        is ContentResult.Loading -> {
                            // Keep loading state
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching data", e)
                _tourPass.value = ContentResult.Error(
                    UiText.Plain(e.message ?: context.getString(R.string.unknown_error)),
                    e
                )
            }
        }
    }
}
