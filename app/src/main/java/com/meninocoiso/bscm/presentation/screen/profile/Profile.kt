package com.meninocoiso.bscm.presentation.screen.profile

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.remote.dto.user.SimplifiedUser
import com.meninocoiso.bscm.domain.result.ContentState
import com.meninocoiso.bscm.presentation.screen.details.OnNavigateToDetails
import com.meninocoiso.bscm.presentation.ui.components.layout.Avatar
import com.meninocoiso.bscm.presentation.ui.components.profile.ProfileActivity
import com.meninocoiso.bscm.presentation.ui.components.profile.ProfileCollections
import com.meninocoiso.bscm.presentation.ui.components.profile.ProfileIndicator
import com.meninocoiso.bscm.presentation.ui.components.profile.ProfileLibrary
import com.meninocoiso.bscm.presentation.ui.components.profile.ProfileLikes
import com.meninocoiso.bscm.presentation.ui.modifiers.roundedPolygonClip
import com.meninocoiso.bscm.presentation.ui.modifiers.roundedPolygonShape
import com.meninocoiso.bscm.presentation.viewmodel.ProfileViewModel
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil3.CoilImage
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
data class Profile(val user: SimplifiedUser, val isOwner: Boolean, val isFollowing: Boolean)

@Serializable
data class DeepLinkProfile(val userId: String)

data class ProfileTabItem(val contentDescription: String, val iconResId: Int)

@Composable
fun getOwnerTabItems(): List<ProfileTabItem> {
    return listOf(
        ProfileTabItem(
            contentDescription = "Likes",
            iconResId = R.drawable.rounded_favorite_24
        ),
        ProfileTabItem(
            contentDescription = "Collections",
            iconResId = R.drawable.rounded_bookmark_24
        )
    )
}

@Composable
fun getProfileTabItems(): List<ProfileTabItem> {
    return listOf(
        ProfileTabItem(
            contentDescription = "Recent activity",
            iconResId = R.drawable.rounded_search_activity_24
        ),
        ProfileTabItem(
            contentDescription = "User content",
            iconResId = R.drawable.outline_library_music_24
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileScreen(
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
    user: SimplifiedUser,
    isFollowing: Boolean,
    isOwner: Boolean,
    onReturn: () -> Unit,
    onNavigateToDetails: OnNavigateToDetails,
    onNavigateToCollection: (collectionId: String) -> Unit = {},
    profileViewModel: ProfileViewModel = hiltViewModel(),
) {
    val coroutineScope = rememberCoroutineScope()

    val userId = user.id

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val tabItems = if (isOwner) getOwnerTabItems() else getProfileTabItems()

    val horizontalPagerState = rememberPagerState { tabItems.size }

    val section1State by profileViewModel.section1State.collectAsStateWithLifecycle(
        initialValue = ContentState.Loading
    )

    val section2State by profileViewModel.section2State.collectAsStateWithLifecycle(
        initialValue = ContentState.Loading
    )

    val collectionContent by profileViewModel.collectionContent.collectAsStateWithLifecycle()
    val activityContent by profileViewModel.activityContent.collectAsStateWithLifecycle()
    val libraryContent by profileViewModel.libraryContent.collectAsStateWithLifecycle()
    val likedContent by profileViewModel.likedContent.collectAsStateWithLifecycle()
    val isFollowingState by profileViewModel.isFollowingState.collectAsStateWithLifecycle()
    val paginationState by profileViewModel.paginationState.collectAsStateWithLifecycle()

    val activityListState = rememberLazyListState()
    val libraryListState = rememberLazyListState()
    val likesListState = rememberLazyListState()
    val bookmarksListState = rememberLazyListState()
    val collectionsListState = rememberLazyListState()

    // Track header scroll offset
    val headerScrollState = rememberScrollState()
    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    val tabRowHeight = 56.dp

    LaunchedEffect(horizontalPagerState.currentPage, isOwner) {
        profileViewModel.onTabSelected(userId, isOwner, horizontalPagerState.currentPage)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background
                ),
                navigationIcon = {
                    IconButton(
                        modifier = Modifier.padding(end = 12.dp),
                        onClick = { onReturn() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            tint = MaterialTheme.colorScheme.onSurface,
                            contentDescription = stringResource(R.string.return_screen)
                        )
                    }
                },
                actions = {
                    if (isOwner) {
                        IconButton(onClick = { }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = stringResource(R.string.share)
                            )
                        }
                    }
                },
                title = {
                    Text("@${user.username}", style = MaterialTheme.typography.headlineMedium)
                },
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.statusBars
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // HorizontalPager - fills entire space and sits behind everything
            HorizontalPager(
                state = horizontalPagerState,
                modifier = Modifier.fillMaxSize(),
                key = { it },
                beyondViewportPageCount = 1
            ) { index ->
                val nestedScrollConnection = remember {
                    object : NestedScrollConnection {
                        override fun onPreScroll(
                            available: Offset,
                            source: NestedScrollSource
                        ): Offset {
                            val delta = available.y
                            val newOffset =
                                (headerScrollState.value - delta).coerceIn(0f, headerHeightPx)
                            val consumed = headerScrollState.value - newOffset

                            coroutineScope.launch {
                                headerScrollState.scrollTo(newOffset.toInt())
                            }

                            return Offset(0f, consumed)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection)
                ) {
                    // Add top padding to account for header + tabs
                    val headerOffsetPx =
                        (headerHeightPx - headerScrollState.value).coerceAtLeast(0f)
                    val topPadding = with(LocalDensity.current) {
                        headerOffsetPx.toDp() + tabRowHeight
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = topPadding)
                    ) {
                        when (index) {
                            0 -> if (isOwner) {
                                ProfileLikes(
                                    items = likedContent,
                                    state = section1State,
                                    onFetch = { profileViewModel.fetchUserLikes(reset = true) },
                                    onNavigateToDetails = onNavigateToDetails,
                                    listState = likesListState,
                                    isLoadingMore = paginationState.isLoadingMoreLikes,
                                    hasMore = paginationState.hasMoreLikes,
                                    onLoadMore = { profileViewModel.loadMoreLikes() },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                ProfileActivity(
                                    items = activityContent,
                                    state = section1State,
                                    onFetch = {
                                        profileViewModel.fetchProfileActivity(
                                            userId,
                                            reset = true
                                        )
                                    },
                                    listState = activityListState,
                                    isLoadingMore = paginationState.isLoadingMoreActivity,
                                    hasMore = paginationState.hasMoreActivity,
                                    onLoadMore = { profileViewModel.loadMoreActivity(userId) },
                                    onNavigateToDetails = onNavigateToDetails,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            1 -> if (isOwner) {
                                ProfileCollections(
                                    modifier = Modifier.fillMaxSize(),
                                    items = collectionContent,
                                    state = section2State,
                                    onFetch = {
                                        profileViewModel.fetchUserCollections(reset = true)
                                    },
                                    onNavigateToDetails = onNavigateToDetails,
                                    onNavigateToCollection = onNavigateToCollection,
                                    bookmarksListState = bookmarksListState,
                                    collectionsListState = collectionsListState,
                                    isLoadingMoreBookmarks = paginationState.isLoadingMoreBookmarks,
                                    hasMoreBookmarks = paginationState.hasMoreBookmarks,
                                    onLoadMoreBookmarks = { profileViewModel.loadMoreBookmarks() },
                                    isLoadingMoreCollections = paginationState.isLoadingMoreCollections,
                                    hasMoreCollections = paginationState.hasMoreCollections,
                                    onLoadMoreCollections = { profileViewModel.loadMoreCollections() },
                                )
                            } else {
                                ProfileLibrary(
                                    libraryContent,
                                    section2State,
                                    { profileViewModel.fetchProfileLibrary(userId, reset = true) },
                                    onNavigateToDetails,
                                    listState = libraryListState,
                                    isLoadingMore = paginationState.isLoadingMoreLibrary,
                                    hasMore = paginationState.hasMoreLibrary,
                                    onLoadMore = { profileViewModel.loadMoreLibrary(userId) },
                                    Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            // Header and TabRow overlay on top
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Collapsible Header - this scrolls up
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(0, -headerScrollState.value) }
                        .onSizeChanged { size ->
                            headerHeightPx = size.height.toFloat()
                        }
                ) {
                    // Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                    when (user.bannerUrl) {
                        null -> Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .size(180.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .zIndex(1f)
                        )

                        else -> CoilImage(
                            imageModel = { user.bannerUrl },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .size(180.dp)
                                    .clip(RoundedCornerShape(28.dp))
                                    .zIndex(1f),
                                imageOptions = ImageOptions(
                                    contentScale = ContentScale.Crop,
                                    alignment = Alignment.Center,
                                ),
                            )
                        }

                    Avatar(
                        url = user.avatarUrl,
                        size = 96.dp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = 16.dp, y = (-16).dp)
                            .zIndex(1f)
                            .then(
                                if (sharedTransitionScope != null && animatedContentScope != null) {
                                    with(sharedTransitionScope) {
                                        Modifier.sharedElement(
                                            rememberSharedContentState(key = "profile_image"),
                                            animatedVisibilityScope = animatedContentScope
                                        )
                                    }
                                } else Modifier
                            )
                            .border(
                                width = 2.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = roundedPolygonShape()
                            )
                            .roundedPolygonClip(),
                    )
                    ProfileIndicator(
                        modifier = Modifier
                            .then(
                                if (sharedTransitionScope != null && animatedContentScope != null) {
                                    with(sharedTransitionScope) {
                                        Modifier.sharedElement(
                                            rememberSharedContentState(key = "profile_icon"),
                                            animatedVisibilityScope = animatedContentScope
                                        )
                                    }
                                } else Modifier
                            )
                            .graphicsLayer(
                                alpha = 0f,
                                scaleX = 0f,
                                scaleY = 0f
                            )
                    )
                }

                    // Actions
                    if (!isOwner) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { profileViewModel.toggleFollow(userId) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    modifier = Modifier.size(20.dp),
                                    painter = painterResource(R.drawable.rounded_stars_24),
                                    contentDescription = null
                                )
                                Text(
                                    modifier = Modifier.padding(start = 8.dp),
                                    text = if (isFollowing) "Seguindo" else "Seguir"
                                )
                            }
                            IconButton(
                                onClick = {}, colors = IconButtonDefaults.iconButtonColors(
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
                    }
                }

                // Sticky TabRow - stays at the top, doesn't move with header
                PrimaryTabRow(
                    modifier = Modifier.offset {
                        IntOffset(0, (-headerScrollState.value).coerceAtMost(0))
                    },
                    selectedTabIndex = horizontalPagerState.currentPage,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                    containerColor = MaterialTheme.colorScheme.background,
                    indicator = {
                        SecondaryIndicator(
                            Modifier.tabIndicatorOffset(
                                horizontalPagerState.currentPage,
                                matchContentSize = false
                            )
                        )
                    }
                ) {
                    tabItems.forEachIndexed { index, item ->
                        Tab(
                            modifier = Modifier.height(tabRowHeight),
                            selected = horizontalPagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    horizontalPagerState.animateScrollToPage(index)
                                }
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(item.iconResId),
                                    contentDescription = item.contentDescription
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

typealias OnNavigateToProfile = (user: SimplifiedUser, isOwner: Boolean, isFollowing: Boolean) -> Unit
