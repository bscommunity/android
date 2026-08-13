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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "TourPassManager"

/**
 * Tour pass-specific manager. Tour passes are not versionable and have no
 * update lifecycle: it handles the remote feed + search with a local cache
 * for offline display, and owns the single source of truth for "which tour
 * passes the user downloaded as a tour pass" ([installedTourPassIds]). That
 * flag is a live signal, seeded from the root manifest at construction and
 * updated optimistically on install/uninstall, so feed writes can never
 * clobber it.
 */
@Singleton
class TourPassManager @Inject constructor(
    private val remoteRepository: TourPassRemoteRepository,
    private val localRepository: TourPassLocalRepository,
    private val tourPassStorageManager: TourPassStorageManager,
    @param:ApplicationScope private val coroutineScope: CoroutineScope,
) {
    private val _feedState = MutableStateFlow<ContentState>(ContentState.Loading)
    val feedState: StateFlow<ContentState> = _feedState.asStateFlow()

    /**
     * Pure feed data, exactly what the server returned. It never carries the
     * installed flag: that lives in [_installedTourPassIds] and is merged in
     * by [tourPassesUiState].
     */
    private val _tourPasses = MutableStateFlow<List<TourPass>>(emptyList())
    val tourPasses: StateFlow<List<TourPass>> = _tourPasses.asStateFlow()

    private val _searchTourPasses = MutableStateFlow<List<TourPass>?>(null)
    val searchTourPasses: StateFlow<List<TourPass>?> = _searchTourPasses.asStateFlow()

    /**
     * Ids of every tour pass the user downloaded as a tour pass (persisted in
     * the root manifest). This is the one place install truth lives: seeded
     * from the manifest at construction, updated optimistically the instant a
     * tour pass is installed or uninstalled, and never derived from or stored
     * in feed data.
     */
    private val _installedTourPassIds = MutableStateFlow<Set<String>>(emptySet())
    val installedTourPassIds: StateFlow<Set<String>> = _installedTourPassIds.asStateFlow()

    /**
     * Whether the manifest seed for [installedTourPassIds] has landed. The ids
     * are seeded asynchronously at construction; until then an empty set is
     * indistinguishable from "nothing installed", so consumers may bridge the
     * gap with the payload's own install flag. Once seeded, the live set is
     * the single source of truth and stale payload flags must be ignored.
     */
    @Volatile
    private var installedIdsSeeded = false

    fun hasSeededInstalledIds(): Boolean = installedIdsSeeded

    /**
     * Reactive flow of every tour pass cached in the local database. Used to
     * derive the list of downloaded tour passes.
     */
    val cachedTourPasses: Flow<List<TourPass>> = localRepository.observeTourPasses()

    /**
     * Feed merged with the installed ids, so consumers always read the
     * correct install status no matter which feed write happened last. The
     * combine re-emits as soon as [installedTourPassIds] changes, so a cold
     * start self-heals once the manifest seed lands.
     */
    val tourPassesUiState: StateFlow<List<TourPass>> =
        combine(_tourPasses, _installedTourPassIds) { feed, installedIds ->
            feed.map { it.copy(isInstalled = it.id in installedIds) }
        }.stateIn(coroutineScope, SharingStarted.Eagerly, emptyList())

    /**
     * Search results merged with the installed ids, same as [tourPassesUiState].
     */
    val searchTourPassesUiState: StateFlow<List<TourPass>?> =
        combine(_searchTourPasses, _installedTourPassIds) { search, installedIds ->
            search?.map { it.copy(isInstalled = it.id in installedIds) }
        }.stateIn(coroutineScope, SharingStarted.Eagerly, null)

    init {
        refreshInstalledIds()
    }

    private fun refreshInstalledIds() {
        coroutineScope.launch {
            _installedTourPassIds.value = withContext(Dispatchers.IO) {
                tourPassStorageManager.readInstalledTourPasses().mapTo(mutableSetOf()) { it.id }
            }
            installedIdsSeeded = true
        }
    }

    fun updateFeedState(newState: ContentState) {
        _feedState.value = newState
    }

    /**
     * Marks a tour pass as installed once all of its charts have been
     * downloaded. Updates the live installed-ids signal immediately (no I/O)
     * and persists the flag locally in the background.
     */
    fun markInstalled(id: String) {
        _installedTourPassIds.value = _installedTourPassIds.value + id
        coroutineScope.launch {
            try {
                val result = localRepository.getTourPass(id).first()
                val tourPass = result.getOrNull() ?: return@launch
                localRepository.update(listOf(tourPass.copy(isInstalled = true))).first()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark tour pass as installed: $id", e)
            }
        }
    }

    /**
     * Drops a tour pass from the live installed-ids signal immediately.
     * Persistence is handled by the caller.
     */
    fun markUninstalled(id: String) {
        _installedTourPassIds.value = _installedTourPassIds.value - id
    }

    /**
     * Synchronous lookup of a tour pass in the in-memory cache, with the
     * installed flag merged from [installedTourPassIds]. Lets the installed
     * status resolve on the very first frame of a details screen without
     * waiting for a database read.
     */
    fun getTourPassFromStore(id: String): TourPass? =
        _tourPasses.value.firstOrNull { it.id == id }?.let { stored ->
            if (stored.id in _installedTourPassIds.value) stored.copy(isInstalled = true) else stored
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
