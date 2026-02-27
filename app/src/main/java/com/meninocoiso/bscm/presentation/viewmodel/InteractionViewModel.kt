package com.meninocoiso.bscm.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.data.local.dao.CollectionDao
import com.meninocoiso.bscm.data.remote.dto.collection.SimplifiedCollection
import com.meninocoiso.bscm.domain.enums.CollectionKind
import com.meninocoiso.bscm.domain.repository.InteractionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InteractionViewModel @Inject constructor(
    private val interactionRepository: InteractionRepository,
    private val collectionDao: CollectionDao
) : ViewModel() {
    
    private val _queueSize = MutableStateFlow(0)
    val queueSize: StateFlow<Int> = _queueSize.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    init {
        updateQueueSize()
    }

    /**
     * Likes content using the offline-first system.
     * Updates local database immediately, queues for remote sync.
     */
    fun likeContent(id: String, contentId: String) {
        viewModelScope.launch {
            interactionRepository.likeContent(id, contentId)
                .onSuccess {
                    updateQueueSize()
                }.onFailure { error ->
                    // Handle error if needed
                }
        }
    }

    /**
     * Unlikes content using the offline-first system.
     * Updates local database immediately, queues for remote sync.
     */
    fun unlikeContent(id: String, contentId: String) {
        viewModelScope.launch {
            interactionRepository.unlikeContent(id, contentId)
                .onSuccess {
                    updateQueueSize()
                }.onFailure { error ->
                    // Handle error if needed
                }
        }
    }

    /**
     * Bookmarks content using the offline-first system.
     * Updates local database immediately, queues for remote sync.
     */
    fun bookmarkContent(id: String, contentId: String) {
        viewModelScope.launch {
            interactionRepository.bookmarkContent(id, contentId)
                .onSuccess {
                    updateQueueSize()
                }.onFailure {
                    // Handle error if needed
                }
        }
    }

    /**
     * Unbookmarks content using the offline-first system.
     * Updates local database immediately, queues for remote sync.
     */
    fun unbookmarkContent(id: String, contentId: String) {
        viewModelScope.launch {
            Log.d("InteractionViewModel", "Attempting to unbookmark contentId=$contentId for id=$id")
            interactionRepository.unbookmarkContent(id, contentId)
                .onSuccess {
                    updateQueueSize()
                }.onFailure {
                    // Handle error if needed
                }
        }
    }

    /**
     * Adds content to a custom collection using the offline-first queue system
     */
    fun addToCollection(id: String, contentId: String, collectionId: String) {
        viewModelScope.launch {
            interactionRepository.addToCollection(id, contentId, collectionId)
                .onSuccess {
                    updateQueueSize()
                }.onFailure {
                    // Handle error if needed
                }
        }
    }

    /**
     * Removes content from a custom collection using the offline-first queue system
     */
    fun removeFromCollection(id: String, contentId: String, collectionId: String) {
        viewModelScope.launch {
            Log.d("InteractionViewModel", "Attempting to remove contentId=$contentId from collectionId=$collectionId")
            interactionRepository.removeFromCollection(id, contentId, collectionId)
                .onSuccess {
                    updateQueueSize()
                }.onFailure {
                    // Handle error if needed
                }
        }
    }

    /**
     * Changes the collection of content, removing it from the previous collection
     * (BOOKMARKS) and adding it to the new collection (custom or vice-versa).
     * This ensures proper queue management and prevents sync conflicts.
     */
    fun changeContentCollection(contentId: String, targetCollectionId: String, targetCollectionKind: CollectionKind) {
        viewModelScope.launch {
            interactionRepository.changeContentCollection(contentId, targetCollectionId, targetCollectionKind)
                .onSuccess {
                    updateQueueSize()
                }.onFailure {
                    // Handle error if needed
                }
        }
    }

    // Keeps at most 10 entries; evicts least-recently-used when full
    private val contentCollectionCache = object : LinkedHashMap<String, StateFlow<SimplifiedCollection?>>(
        16,       // initial capacity
        0.75f,    // load factor
        true      // accessOrder = true → makes it LRU
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, StateFlow<SimplifiedCollection?>>
        ) = size > 10
    }

    fun getContentCollection(contentId: String): StateFlow<SimplifiedCollection?> =
        contentCollectionCache.getOrPut(contentId) {
            collectionDao.getCollectionForContent(contentId)
                .distinctUntilChanged()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
        }

    /**
     * Manually processes the interaction queue
     */
    /*fun processQueue() {
        applicationScope.launch {
            _isProcessing.value = true
            try {
                interactionRepository.processQueue()
                updateQueueSize()
            } finally {
                _isProcessing.value = false
            }
        }
    }*/

    /**
     * Updates the current queue size
     */
    private fun updateQueueSize() {
        viewModelScope.launch {
            _queueSize.value = interactionRepository.getQueueSize()
        }
    }
}