package com.meninocoiso.bscm.data.manager

import android.util.Log
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.di.ApplicationScope
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.domain.repository.TourPassLocalRepository
import com.meninocoiso.bscm.domain.repository.TourPassRemoteRepository
import com.meninocoiso.bscm.domain.result.ContentResult
import com.meninocoiso.bscm.domain.result.ContentState
import com.meninocoiso.bscm.domain.result.UiText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TourPassManager"

/**
 * Tour pass-specific manager. Tour passes are not versionable and have no
 * install/update lifecycle, so this manager stays lean: remote feed + search,
 * with a local cache for offline display.
 */
@Singleton
class TourPassManager @Inject constructor(
    private val remoteRepository: TourPassRemoteRepository,
    private val localRepository: TourPassLocalRepository,
    @param:ApplicationScope private val coroutineScope: CoroutineScope,
) {
    private val _feedState = MutableStateFlow<ContentState>(ContentState.Loading)
    val feedState: StateFlow<ContentState> = _feedState.asStateFlow()

    private val _tourPasses = MutableStateFlow<List<TourPass>>(emptyList())
    val tourPasses: StateFlow<List<TourPass>> = _tourPasses.asStateFlow()

    private val _searchTourPasses = MutableStateFlow<List<TourPass>?>(null)
    val searchTourPasses: StateFlow<List<TourPass>?> = _searchTourPasses.asStateFlow()

    /**
     * Reactive flow of every tour pass cached in the local database. Used to
     * derive the list of downloaded tour passes.
     */
    val cachedTourPasses: Flow<List<TourPass>> = localRepository.observeTourPasses()

    fun updateFeedState(newState: ContentState) {
        _feedState.value = newState
    }

    /**
     * Marks a tour pass as installed once all of its charts have been
     * downloaded. Persists the flag locally and updates the in-memory feed.
     */
    fun markInstalled(id: String) {
        coroutineScope.launch {
            try {
                val result = localRepository.getTourPass(id).first()
                val tourPass = result.getOrNull() ?: return@launch
                val updated = tourPass.copy(isInstalled = true)
                localRepository.update(listOf(updated)).first()
                _tourPasses.value = _tourPasses.value.map { tourPass ->
                    if (tourPass.id == id) tourPass.copy(isInstalled = true) else tourPass
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark tour pass as installed: $id", e)
            }
        }
    }

    fun getTourPassesLength(): Int = _tourPasses.value.size

    suspend fun loadCachedTourPasses(limit: Int? = null) {
        _feedState.value = ContentState.Loading
        try {
            val cached = localRepository.getTourPasses(limit = limit).first()
            cached.fold(
                onSuccess = { list ->
                    _tourPasses.value = list
                    _feedState.value = ContentState.Success
                },
                onFailure = {
                    _feedState.value = ContentState.Error
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "loadCachedTourPasses failed", e)
            _feedState.value = ContentState.Error
        }
    }

    fun fetchFeedTourPasses(
        forceRefresh: Boolean = false,
        limit: Int = 10,
        offset: Int = 0,
    ): Flow<ContentResult<List<TourPass>>> = flow {
        if (!forceRefresh && offset == 0 && _tourPasses.value.isNotEmpty()) {
            _feedState.value = ContentState.Success
            emit(ContentResult.Success(_tourPasses.value))
            return@flow
        }

        emit(ContentResult.Loading)
        val remoteResult = remoteRepository.getTourPasses(limit = limit, offset = offset).first()
        remoteResult.fold(
            onSuccess = { remoteItems ->
                _tourPasses.value = if (offset == 0) {
                    remoteItems
                } else {
                    (_tourPasses.value + remoteItems).distinctBy { it.id }
                }
                coroutineScope.launch { localRepository.update(remoteItems).first() }
                _feedState.value = ContentState.Success
                emit(ContentResult.Success(remoteItems))
            },
            onFailure = { err ->
                _feedState.value = ContentState.Error
                emit(
                    ContentResult.Error(
                        err.message?.let { UiText.Plain(it) }
                            ?: UiText.Res(R.string.failed_to_fetch_feed_charts),
                        err
                    )
                )
            }
        )
    }.catch { e ->
        _feedState.value = ContentState.Error
        emit(ContentResult.Error(UiText.Res(R.string.failed_to_fetch_feed_charts), e))
    }

    fun searchTourPasses(
        query: String,
        limit: Int = 10,
        offset: Int = 0,
    ): Flow<ContentResult<List<TourPass>>> = flow {
        if (query.isBlank()) {
            _searchTourPasses.value = null
            emit(ContentResult.Success(emptyList()))
            return@flow
        }

        emit(ContentResult.Loading)
        val remoteResult = remoteRepository.getTourPasses(
            query = query,
            limit = limit,
            offset = offset
        ).first()
        remoteResult.fold(
            onSuccess = { remoteItems ->
                val merged = if (offset == 0) {
                    remoteItems
                } else {
                    (_searchTourPasses.value ?: emptyList()) + remoteItems
                }
                _searchTourPasses.value = merged.distinctBy { it.id }
                coroutineScope.launch { localRepository.update(remoteItems).first() }
                _feedState.value = ContentState.Success
                emit(ContentResult.Success(remoteItems))
            },
            onFailure = { err ->
                _feedState.value = ContentState.Error
                emit(
                    ContentResult.Error(
                        err.message?.let { UiText.Plain(it) }
                            ?: UiText.Res(R.string.search_failed),
                        err
                    )
                )
            }
        )
    }.catch { e ->
        _feedState.value = ContentState.Error
        emit(ContentResult.Error(UiText.Res(R.string.search_failed), e))
    }

    fun getTourPass(id: String): Flow<ContentResult<TourPass>> = flow {
        emit(ContentResult.Loading)
        val localResult = localRepository.getTourPass(id).first()
        localResult.fold(
            onSuccess = { tourPass ->
                emit(ContentResult.Success(tourPass))
            },
            onFailure = {
                val remoteResult = remoteRepository.getTourPass(id).first()
                remoteResult.fold(
                    onSuccess = { tourPass ->
                        coroutineScope.launch { localRepository.insert(listOf(tourPass)).first() }
                        emit(ContentResult.Success(tourPass))
                    },
                    onFailure = { err ->
                        emit(
                            ContentResult.Error(
                                err.message?.let { UiText.Plain(it) }
                                    ?: UiText.Res(R.string.content_not_found),
                                err
                            )
                        )
                    }
                )
            }
        )
    }

    fun clearSearch() {
        _searchTourPasses.value = null
    }

    fun clearCache() {
        _tourPasses.value = emptyList()
        _searchTourPasses.value = null
        _feedState.value = ContentState.Loading
    }
}
