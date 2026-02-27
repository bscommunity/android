package com.meninocoiso.bscm.presentation.screen.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.remote.dto.user.SimplifiedUser
import com.meninocoiso.bscm.domain.model.SimplifiedCollection
import com.meninocoiso.bscm.domain.model.toSimplifiedCollection
import com.meninocoiso.bscm.presentation.screen.details.OnNavigateToDetails
import com.meninocoiso.bscm.presentation.ui.components.dialog.ReportDialog
import com.meninocoiso.bscm.presentation.ui.components.profile.ProfileActivity
import com.meninocoiso.bscm.presentation.ui.components.profile.ProfileHeaderIdentity
import com.meninocoiso.bscm.presentation.ui.components.profile.ProfileLibrary
import com.meninocoiso.bscm.presentation.ui.components.profile.ProfileSectionsLayout
import com.meninocoiso.bscm.presentation.ui.components.profile.ProfileTabItem
import com.meninocoiso.bscm.presentation.viewmodel.PublicProfileViewModel
import com.meninocoiso.bscm.util.LinkingUtils
import kotlinx.serialization.Serializable

@Serializable
data class PublicProfile(val user: SimplifiedUser, val isFollowing: Boolean)

@Serializable
data class DeepLinkProfile(val username: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    isLoggedIn: Boolean,
    user: SimplifiedUser,
    onReturn: () -> Unit,
    onNavigateToDetails: OnNavigateToDetails,
    onNavigateToCollection: (SimplifiedCollection) -> Unit,
    profileViewModel: PublicProfileViewModel,
) {
    val userId = user.id
    val context = LocalContext.current

    val tabItems = listOf(
        ProfileTabItem(
            contentDescription = "Recent activity",
            iconResId = R.drawable.rounded_search_activity_24
        ),
        ProfileTabItem(
            contentDescription = "User content",
            iconResId = R.drawable.outline_library_music_24
        )
    )

    val uiState by profileViewModel.uiState.collectAsStateWithLifecycle()

    val isReportDialogOpen = remember { mutableStateOf(false) }

    val activityListState = rememberLazyListState()
    val libraryListState = rememberLazyListState()
    val collectionsListState = rememberLazyListState()

    ProfileSectionsLayout(
        user = user,
        tabItems = tabItems,
        onReturn = onReturn,
        headerIdentity = { ProfileHeaderIdentity(user = user) },
        headerActions = {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isLoggedIn) {
                    Button(
                        onClick = { profileViewModel.toggleFollow(userId) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            painter = if (uiState.isFollowing)
                                painterResource(R.drawable.baseline_stars_24)
                            else
                                painterResource(R.drawable.rounded_stars_24),
                            contentDescription = null
                        )
                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = if (uiState.isFollowing) "Following" else "Follow"
                        )
                    }
                    IconButton(
                        onClick = {
                            LinkingUtils.shareProfile(
                                context = context,
                                username = user.username
                            )
                        }, colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = Icons.Outlined.Share,
                            contentDescription = null
                        )
                    }
                    if (!uiState.isFollowing) {
                        IconButton(
                            onClick = {}, colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(R.drawable.rounded_flag_24),
                                contentDescription = null
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { LinkingUtils.shareProfile(context, user.username) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            modifier = Modifier.size(20.dp),
                            imageVector = Icons.Outlined.Share,
                            contentDescription = null
                        )
                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = stringResource(R.string.share)
                        )
                    }
                }
            }
        },
        onTabSelected = { index -> profileViewModel.onTabSelected(userId, index) },
    ) { index ->
        when (index) {
            0 -> {
                ProfileActivity(
                    items = uiState.activity.items,
                    state = uiState.activity.state,
                    onFetch = {
                        profileViewModel.refreshActivity(userId)
                    },
                    listState = activityListState,
                    isLoadingMore = uiState.activity.isLoadingMore,
                    hasMore = uiState.activity.hasMore,
                    onLoadMore = { profileViewModel.loadMoreActivity(userId) },
                    onNavigateToDetails = onNavigateToDetails,
                    modifier = Modifier.fillMaxSize()
                )
            }

            1 -> {
                ProfileLibrary(
                    items = uiState.library.items,
                    state = uiState.library.state,
                    isRefreshing = uiState.library.isRefreshing,
                    customCollections = uiState.customCollections,
                    onFetch = {
                        profileViewModel.refreshLibrary(userId)
                        profileViewModel.refreshCollections(userId)
                    },
                    onNavigateToDetails = onNavigateToDetails,
                    onNavigateToCollection = { collection ->
                        onNavigateToCollection(collection.toSimplifiedCollection(user))
                    },
                    listState = libraryListState,
                    collectionsListState = collectionsListState,
                    isLoadingMore = uiState.library.isLoadingMore,
                    hasMore = uiState.library.hasMore,
                    onLoadMore = { profileViewModel.loadMoreLibrary(userId) },
                    isLoadingMoreCollections = uiState.customCollections.isLoadingMore,
                    hasMoreCollections = uiState.customCollections.hasMore,
                    onLoadMoreCollections = { profileViewModel.loadMoreCollections(userId) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    if (isReportDialogOpen.value) {
        ReportDialog(
            onSubmit = {},
            onDismiss = { isReportDialogOpen.value = false },
        )
    }
}
