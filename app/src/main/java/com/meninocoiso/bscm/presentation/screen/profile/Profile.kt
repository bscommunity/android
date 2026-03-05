package com.meninocoiso.bscm.presentation.screen.profile

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.remote.dto.user.SimplifiedUser
import com.meninocoiso.bscm.domain.model.SimplifiedCollection
import com.meninocoiso.bscm.domain.model.toSimplifiedCollection
import com.meninocoiso.bscm.presentation.screen.details.OnNavigateToDetails
import com.meninocoiso.bscm.presentation.ui.components.profile.ProfileCollections
import com.meninocoiso.bscm.presentation.ui.components.profile.ProfileHeaderIdentity
import com.meninocoiso.bscm.presentation.ui.components.profile.ProfileLikes
import com.meninocoiso.bscm.presentation.ui.components.profile.ProfileSectionsLayout
import com.meninocoiso.bscm.presentation.ui.components.profile.ProfileTabItem
import com.meninocoiso.bscm.presentation.ui.utils.resolve
import com.meninocoiso.bscm.presentation.viewmodel.profile.UserProfileViewModel
import com.meninocoiso.bscm.util.LinkingUtils
import kotlinx.serialization.Serializable

@Serializable
data class Profile(val user: SimplifiedUser)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileScreen(
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
    user: SimplifiedUser,
    onReturn: () -> Unit,
    onNavigateToDetails: OnNavigateToDetails,
    onNavigateToCollection: (SimplifiedCollection) -> Unit = {},
    profileViewModel: UserProfileViewModel = hiltViewModel(),
) {
    val context = LocalContext.current

    val tabItems = listOf(
        ProfileTabItem(
            contentDescription = stringResource(R.string.likes),
            iconResId = R.drawable.rounded_favorite_24
        ),
        ProfileTabItem(
            contentDescription = stringResource(R.string.collections),
            iconResId = R.drawable.rounded_bookmark_24
        )
    )

    val uiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    // val profile by profileViewModel.profile.collectAsStateWithLifecycle()

    val likesListState = rememberLazyListState()
    val bookmarksListState = rememberLazyListState()
    val collectionsListState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(profileViewModel) {
        profileViewModel.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message.resolve(context))
        }
    }

    ProfileSectionsLayout(
        user = user,
        tabItems = tabItems,
        onReturn = onReturn,
        snackbarHostState = snackbarHostState,
        topBarActions = {
            IconButton(onClick = {
                LinkingUtils.shareProfile(
                    context = context,
                    username = user.username,
                )
            }) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = stringResource(R.string.share)
                )
            }
        },
        headerIdentity = {
            ProfileHeaderIdentity(
                user = user,
                sharedTransitionScope = sharedTransitionScope,
                animatedContentScope = animatedContentScope
            )
        },
        onTabSelected = profileViewModel::onTabSelected,
    ) { index ->
        when (index) {
            0 -> {
                ProfileLikes(
                    items = uiState.likes.items,
                    counts = uiState.likesCounts,
                    state = uiState.likes.state,
                    isRefreshing = uiState.likes.isRefreshing,
                    onFetch = { profileViewModel.refreshUserLikes() },
                    onNavigateToDetails = onNavigateToDetails,
                    listState = likesListState,
                    isLoadingMore = uiState.likes.isLoadingMore,
                    hasMore = uiState.likes.hasMore,
                    onLoadMore = { profileViewModel.loadMoreLikes() },
                    modifier = Modifier.fillMaxSize()
                )
            }

            1 -> {
                ProfileCollections(
                    modifier = Modifier.fillMaxSize(),
                    bookmarks = uiState.collections.bookmarks,
                    bookmarksCounts = uiState.bookmarksCounts,
                    customCollections = uiState.collections.customCollections,
                    collectionsCount = uiState.collectionsCount,
                    isRefreshing = uiState.collections.isRefreshing,
                    onFetch = { profileViewModel.refreshUserCollections() },
                    onNavigateToDetails = onNavigateToDetails,
                    onNavigateToCollection = { collection ->
                        onNavigateToCollection(
                            collection.toSimplifiedCollection(user)
                        )
                    },
                    bookmarksListState = bookmarksListState,
                    collectionsListState = collectionsListState,
                    isLoadingMoreBookmarks = uiState.collections.bookmarks.isLoadingMore,
                    hasMoreBookmarks = uiState.collections.bookmarks.hasMore,
                    onLoadMoreBookmarks = { profileViewModel.loadMoreBookmarks() },
                    isLoadingMoreCollections = uiState.collections.customCollections.isLoadingMore,
                    hasMoreCollections = uiState.collections.customCollections.hasMore,
                    onLoadMoreCollections = { profileViewModel.loadMoreCollections() },
                )
            }
        }
    }
}

typealias OnNavigateToProfile = (user: SimplifiedUser) -> Unit