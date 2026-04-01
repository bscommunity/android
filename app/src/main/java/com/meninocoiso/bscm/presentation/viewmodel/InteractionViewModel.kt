package com.meninocoiso.bscm.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meninocoiso.bscm.data.local.dao.CollectionDao
import com.meninocoiso.bscm.data.remote.dto.collection.SimplifiedCollection
import com.meninocoiso.bscm.domain.repository.InteractionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val INTERACTION_DEBOUNCE_MILLIS = 600L

@HiltViewModel
class InteractionViewModel @Inject constructor(
    private val interactionRepository: InteractionRepository,
    private val collectionDao: CollectionDao
) : ViewModel() {

    private val _queueSize = MutableStateFlow(0)
    val queueSize: StateFlow<Int> = _queueSize.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    // -------------------------------------------------------------------------
    // Bookmark debounce state
    // -------------------------------------------------------------------------
    private var bookmarkMutationJob: Job? = null
    private var pendingBookmarkMutation: Boolean? = null

    fun enqueueBookmarkMutation(id: String, contentId: String, isBookmarked: Boolean) {
        pendingBookmarkMutation = isBookmarked
        bookmarkMutationJob?.cancel()
        bookmarkMutationJob = viewModelScope.launch {
            delay(INTERACTION_DEBOUNCE_MILLIS)
            commitBookmarkMutation(id, contentId, isBookmarked)
            pendingBookmarkMutation = null
            bookmarkMutationJob = null
        }
    }

    fun flushPendingBookmarkMutation(id: String, contentId: String) {
        bookmarkMutationJob?.cancel()
        val pending = pendingBookmarkMutation ?: return
        pendingBookmarkMutation = null
        bookmarkMutationJob = null
        viewModelScope.launch { commitBookmarkMutation(id, contentId, pending) }
    }

    private suspend fun commitBookmarkMutation(id: String, contentId: String, isBookmarked: Boolean) {
        if (isBookmarked) bookmarkContent(id, contentId)
        else unbookmarkContent(id, contentId)
    }

    // -------------------------------------------------------------------------
    // Like debounce state
    // -------------------------------------------------------------------------
    private var likeMutationJob: Job? = null
    private var pendingLikeMutation: Boolean? = null

    fun enqueueLikeMutation(id: String, contentId: String, isLiked: Boolean) {
        pendingLikeMutation = isLiked
        likeMutationJob?.cancel()
        likeMutationJob = viewModelScope.launch {
            delay(INTERACTION_DEBOUNCE_MILLIS)
            if (isLiked) likeContent(id, contentId)
            else unlikeContent(id, contentId)
            pendingLikeMutation = null
            likeMutationJob = null
        }
    }

    // -------------------------------------------------------------------------
    // Core interaction operations
    // -------------------------------------------------------------------------
    fun likeContent(id: String, contentId: String) {
        viewModelScope.launch {
            interactionRepository.likeContent(id, contentId)
                .onSuccess { updateQueueSize() }
        }
    }

    fun unlikeContent(id: String, contentId: String) {
        viewModelScope.launch {
            interactionRepository.unlikeContent(id, contentId)
                .onSuccess { updateQueueSize() }
        }
    }

    fun bookmarkContent(id: String, contentId: String) {
        viewModelScope.launch {
            interactionRepository.bookmarkContent(id, contentId)
                .onSuccess { updateQueueSize() }
        }
    }

    fun unbookmarkContent(id: String, contentId: String) {
        viewModelScope.launch {
            Log.d("InteractionViewModel", "Unbookmarking contentId=$contentId for id=$id")
            interactionRepository.unbookmarkContent(id, contentId)
                .onSuccess { updateQueueSize() }
        }
    }

    fun addToCollection(id: String, contentId: String, collectionId: String) {
        viewModelScope.launch {
            interactionRepository.addToCollection(id, contentId, collectionId)
                .onSuccess { updateQueueSize() }
        }
    }

    fun removeFromCollection(id: String, contentId: String, collectionId: String) {
        viewModelScope.launch {
            Log.d("InteractionViewModel", "Removing contentId=$contentId from collectionId=$collectionId")
            interactionRepository.removeFromCollection(id, contentId, collectionId)
                .onSuccess { updateQueueSize() }
        }
    }

    // -------------------------------------------------------------------------
    // Collections cache
    // -------------------------------------------------------------------------
    private val contentCollectionsCache = object : LinkedHashMap<String, StateFlow<List<SimplifiedCollection>>>(
        16, 0.75f, true
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, StateFlow<List<SimplifiedCollection>>>
        ) = size > 10
    }

    fun getContentCollections(contentId: String): StateFlow<List<SimplifiedCollection>> =
        contentCollectionsCache.getOrPut(contentId) {
            collectionDao.getCollectionsForContent(contentId)
                .distinctUntilChanged()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

    // -------------------------------------------------------------------------
    // Queue
    // -------------------------------------------------------------------------
    init { updateQueueSize() }

    private fun updateQueueSize() {
        viewModelScope.launch {
            _queueSize.value = interactionRepository.getQueueSize()
        }
    }
}