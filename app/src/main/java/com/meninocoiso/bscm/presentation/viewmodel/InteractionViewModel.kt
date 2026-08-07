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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val INTERACTION_DEBOUNCE_MILLIS = 600L

/**
 * ViewModel that debounces user interactions and forwards them to the offline-first repository.
 */
@HiltViewModel
class InteractionViewModel @Inject constructor(
    private val interactionRepository: InteractionRepository,
    private val collectionDao: CollectionDao
) : ViewModel() {

    // -------------------------------------------------------------------------
    // Bookmark debounce state
    // -------------------------------------------------------------------------
    private var bookmarkMutationJob: Job? = null
    private var pendingBookmarkMutation: Boolean? = null

    /**
     * Debounces bookmark toggles so fast taps collapse into a single final mutation.
     */
    fun enqueueBookmarkMutation(id: String, isBookmarked: Boolean) {
        pendingBookmarkMutation = isBookmarked
        bookmarkMutationJob?.cancel()
        bookmarkMutationJob = viewModelScope.launch {
            // Delay before commit to absorb rapid toggle bursts.
            delay(INTERACTION_DEBOUNCE_MILLIS)
            commitBookmarkMutation(id, isBookmarked)
            pendingBookmarkMutation = null
            bookmarkMutationJob = null
        }
    }

    /**
     * Immediately executes the last pending bookmark mutation (used before opening manage UI).
     */
    fun flushPendingBookmarkMutation(id: String) {
        bookmarkMutationJob?.cancel()
        val pending = pendingBookmarkMutation ?: return
        pendingBookmarkMutation = null
        bookmarkMutationJob = null
        viewModelScope.launch { commitBookmarkMutation(id, pending) }
    }

    /** Commits the final bookmark state after debounce. */
    private fun commitBookmarkMutation(id: String, isBookmarked: Boolean) {
        if (isBookmarked) bookmarkContent(id)
        else unbookmarkContent(id)
    }

    // -------------------------------------------------------------------------
    // Like debounce state
    // -------------------------------------------------------------------------
    private var likeMutationJob: Job? = null
    private var pendingLikeMutation: Boolean? = null

    /** Debounces like toggles to avoid unnecessary queue churn from rapid taps. */
    fun enqueueLikeMutation(id: String, isLiked: Boolean) {
        pendingLikeMutation = isLiked
        likeMutationJob?.cancel()
        likeMutationJob = viewModelScope.launch {
            delay(INTERACTION_DEBOUNCE_MILLIS)
            if (isLiked) likeContent(id)
            else unlikeContent(id)
            pendingLikeMutation = null
            likeMutationJob = null
        }
    }

    // -------------------------------------------------------------------------
    // Core interaction operations
    // -------------------------------------------------------------------------
    /** Queues a like mutation via repository and refreshes queue size metrics. */
    fun likeContent(id: String) {
        viewModelScope.launch {
            interactionRepository.likeContent(id)
                .onSuccess { }
        }
    }

    /** Queues an unlike mutation via repository and refreshes queue size metrics. */
    fun unlikeContent(id: String) {
        viewModelScope.launch {
            interactionRepository.unlikeContent(id)
                .onSuccess { }
        }
    }

    /** Queues a bookmark mutation via repository and refreshes queue size metrics. */
    fun bookmarkContent(id: String) {
        viewModelScope.launch {
            interactionRepository.bookmarkContent(id)
                .onSuccess { }
        }
    }

    /** Queues an unbookmark mutation via repository and refreshes queue size metrics. */
    fun unbookmarkContent(id: String) {
        viewModelScope.launch {
            Log.d("InteractionViewModel", "Unbookmarking id=$id")
            interactionRepository.unbookmarkContent(id)
                .onSuccess { }
        }
    }

    /** Queues add-to-collection mutation via repository. */
    fun addToCollection(id: String, collectionId: String) {
        viewModelScope.launch {
            interactionRepository.addToCollection(id, collectionId)
                .onSuccess { }
        }
    }

    /** Queues remove-from-collection mutation via repository. */
    fun removeFromCollection(id: String, collectionId: String) {
        viewModelScope.launch {
            Log.d("InteractionViewModel", "Removing id=$id from collectionId=$collectionId")
            interactionRepository.removeFromCollection(id, collectionId)
                .onSuccess { }
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

    /**
     * Returns cached flow of collection memberships for one catalog id.
     *
     * A small LRU map avoids recreating identical Room subscriptions repeatedly.
     */
    fun getContentCollections(id: String): StateFlow<List<SimplifiedCollection>> =
        contentCollectionsCache.getOrPut(id) {
            collectionDao.getCollectionsForContent(id)
                .distinctUntilChanged()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
        }

}