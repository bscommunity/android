package com.meninocoiso.bscm.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.data.repository.InteractionRepository
import com.meninocoiso.bscm.domain.enums.ContentType
import com.meninocoiso.bscm.domain.model.InteractionResult
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
    fun likeContent(contentType: ContentType, contentId: ULong) {
        viewModelScope.launch {
            interactionRepository.likeContent(contentType, contentId)
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
    fun unlikeContent(contentType: ContentType, contentId: ULong) {
        viewModelScope.launch {
            interactionRepository.unlikeContent(contentType, contentId)
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
    fun getLikeStatus(contentType: ContentType, contentId: ULong): Flow<Result<Boolean>> {
        return interactionRepository.isContentLiked(contentType, contentId)
    }
    
    /**
     * Bookmarks content using the offline-first queue system
     */
    fun bookmarkContent(
        contentType: ContentType, 
        contentId: ULong, 
        collectionId: ULong, 
        userId: String
    ) {
        viewModelScope.launch {
            interactionRepository.bookmarkContent(contentType, contentId, collectionId, userId)
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
    fun unbookmarkContent(
        contentType: ContentType, 
        contentId: ULong, 
        collectionId: ULong, 
        userId: String
    ) {
        viewModelScope.launch {
            interactionRepository.unbookmarkContent(contentType, contentId, collectionId, userId)
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
