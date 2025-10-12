package com.meninocoiso.bscm.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.domain.repository.InteractionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InteractionViewModel @Inject constructor(
    private val interactionRepository: InteractionRepository
) : ViewModel() {
    
    private val _queueSize = MutableStateFlow(0)
    val queueSize: StateFlow<Int> = _queueSize.asStateFlow()
    
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()
    
    init {
        updateQueueSize()
    }
    
    /**
     * Likes content using the offline-first queue system
     */
    fun likeContent(contentId: String) {
        viewModelScope.launch {
            interactionRepository.likeContent(contentId)
                .collect { result ->
                    result.onSuccess { 
                        updateQueueSize()
                    }.onFailure { error ->
                        // Handle error if needed
                    }
                }
        }
    }
    
    /**
     * Unlikes content using the offline-first queue system
     */
    fun unlikeContent(contentId: String) {
        viewModelScope.launch {
            interactionRepository.unlikeContent(contentId)
                .collect { result ->
                    result.onSuccess { 
                        updateQueueSize()
                    }.onFailure { error ->
                        // Handle error if needed
                    }
                }
        }
    }
    
    /**
     * Gets the like status for content
     */
    suspend fun getLikeStatus(contentId: String): Flow<Result<Boolean>> {
        return interactionRepository.isContentLiked(contentId)
    }
    
    /**
     * Bookmarks content using the offline-first queue system
     */
    fun favoriteContent(contentId: String) {
        viewModelScope.launch {
            interactionRepository.favoriteContent(contentId)
                .collect { result ->
                    result.onSuccess { 
                        updateQueueSize()
                    }.onFailure { error ->
                        // Handle error if needed
                    }
                }
        }
    }
    
    /**
     * Unbookmarks content using the offline-first queue system
     */
    fun unfavoriteContent(contentId: String) {
        viewModelScope.launch {
            interactionRepository.unfavoriteContent(contentId)
                .collect { result ->
                    result.onSuccess { 
                        updateQueueSize()
                    }.onFailure { error ->
                        // Handle error if needed
                    }
                }
        }
    }
    
    /**
     * Manually processes the interaction queue
     */
    fun processQueue() {
        viewModelScope.launch {
            _isProcessing.value = true
            try {
                interactionRepository.processQueue()
                updateQueueSize()
            } finally {
                _isProcessing.value = false
            }
        }
    }
    
    /**
     * Updates the current queue size
     */
    private fun updateQueueSize() {
        viewModelScope.launch {
            _queueSize.value = interactionRepository.getQueueSize()
        }
    }
}
