package com.meninocoiso.bscm.data.manager

import android.util.Log
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.di.ApplicationScope
import com.meninocoiso.bscm.domain.model.Theme
import com.meninocoiso.bscm.domain.repository.ThemeLocalRepository
import com.meninocoiso.bscm.domain.repository.ThemeRemoteRepository
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

private const val TAG = "ThemeManager"

/**
 * Theme-specific manager. Mirrors the workshop browse/search pipeline used by
 * tour passes; install/update/version machinery lives in the details screen.
 */
@Singleton
class ThemeManager @Inject constructor(
    private val remoteRepository: ThemeRemoteRepository,
    private val localRepository: ThemeLocalRepository,
    @param:ApplicationScope private val coroutineScope: CoroutineScope,
) {
    private val _feedState = MutableStateFlow<ContentState>(ContentState.Loading)
    val feedState: StateFlow<ContentState> = _feedState.asStateFlow()

    private val _themes = MutableStateFlow<List<Theme>>(emptyList())
    val themes: StateFlow<List<Theme>> = _themes.asStateFlow()

    private val _searchThemes = MutableStateFlow<List<Theme>?>(null)
    val searchThemes: StateFlow<List<Theme>?> = _searchThemes.asStateFlow()

    /**
     * Reactive flow of every theme cached in the local database. Used to show
     * locally available themes on the updates page.
     */
    val cachedThemes: Flow<List<Theme>> = localRepository.observeThemes()

    fun updateFeedState(newState: ContentState) {
        _feedState.value = newState
    }

    fun getThemesLength(): Int = _themes.value.size

    suspend fun loadCachedThemes(limit: Int? = null) {
        _feedState.value = ContentState.Loading
        try {
            val cached = localRepository.getThemes(limit = limit).first()
            cached.fold(
                onSuccess = { list ->
                    _themes.value = list
                    _feedState.value = ContentState.Success
                },
                onFailure = {
                    _feedState.value = ContentState.Error
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "loadCachedThemes failed", e)
            _feedState.value = ContentState.Error
        }
    }

    fun fetchFeedThemes(
        forceRefresh: Boolean = false,
        limit: Int = 10,
        offset: Int = 0,
    ): Flow<ContentResult<List<Theme>>> = flow {
        if (!forceRefresh && offset == 0 && _themes.value.isNotEmpty()) {
            _feedState.value = ContentState.Success
            emit(ContentResult.Success(_themes.value))
            return@flow
        }

        emit(ContentResult.Loading)
        val remoteResult = remoteRepository.getThemes(limit = limit, offset = offset).first()
        remoteResult.fold(
            onSuccess = { remoteItems ->
                _themes.value = if (offset == 0) {
                    remoteItems
                } else {
                    (_themes.value + remoteItems).distinctBy { it.id }
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

    fun searchThemes(
        query: String,
        limit: Int = 10,
        offset: Int = 0,
    ): Flow<ContentResult<List<Theme>>> = flow {
        if (query.isBlank()) {
            _searchThemes.value = null
            emit(ContentResult.Success(emptyList()))
            return@flow
        }

        emit(ContentResult.Loading)
        val remoteResult = remoteRepository.getThemes(
            query = query,
            limit = limit,
            offset = offset
        ).first()
        remoteResult.fold(
            onSuccess = { remoteItems ->
                val merged = if (offset == 0) {
                    remoteItems
                } else {
                    (_searchThemes.value ?: emptyList()) + remoteItems
                }
                _searchThemes.value = merged.distinctBy { it.id }
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

    fun getTheme(id: String): Flow<ContentResult<Theme>> = flow {
        emit(ContentResult.Loading)
        val localResult = localRepository.getTheme(id).first()
        localResult.fold(
            onSuccess = { theme ->
                emit(ContentResult.Success(theme))
            },
            onFailure = {
                val remoteResult = remoteRepository.getTheme(id).first()
                remoteResult.fold(
                    onSuccess = { theme ->
                        coroutineScope.launch { localRepository.insert(listOf(theme)).first() }
                        emit(ContentResult.Success(theme))
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
        _searchThemes.value = null
    }

    fun clearCache() {
        _themes.value = emptyList()
        _searchThemes.value = null
        _feedState.value = ContentState.Loading
    }
}
