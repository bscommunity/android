package com.meninocoiso.bscm.presentation.screen.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.remote.dto.ContributorUserDto
import com.meninocoiso.bscm.domain.enums.Difficulty
import com.meninocoiso.bscm.domain.enums.Role
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Contributor
import com.meninocoiso.bscm.domain.model.Theme
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.domain.model.User
import com.meninocoiso.bscm.domain.model.Version
import com.meninocoiso.bscm.domain.result.ContentState
import com.meninocoiso.bscm.presentation.ui.components.ContentFilterOption
import com.meninocoiso.bscm.presentation.ui.components.ContentFilterUI
import com.meninocoiso.bscm.presentation.ui.components.SegmentedButtonUI
import com.meninocoiso.bscm.presentation.ui.components.layout.Avatar
import com.meninocoiso.bscm.presentation.ui.components.preview.ChartPreview
import com.meninocoiso.bscm.presentation.ui.modifiers.roundedPolygonClip
import com.meninocoiso.bscm.presentation.ui.modifiers.roundedPolygonShape
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil3.CoilImage
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.util.Date

@Serializable
data class Profile(val user: User)

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

data class ActivityItem(val date: Date, val content: List<CatalogItem>)

val placeholderChart = Chart(
    id = "placeholder_id",
    contentId = "share_placeholder",
    artist = "Artista Fictício",
    track = "Música Exemplo",
    album = "Álbum Exemplo",
    genre = null,
    coverUrl = "",
    trackUrls = emptyList(),
    trackPreviewUrl = "",
    isFeatured = false,
    isInstalled = false,
    downloadsSum = 0,
    latestPublishedAt = LocalDateTime.now(),
    latestVersion = Version(
        id = 1L,
        chartId = "placeholder_chart_id",
        index = 1,
        duration = 180f,
        notesAmount = 1000,
        effectsAmount = 50,
        bpm = 128,
        difficulty = Difficulty.NORMAL,
        isDeluxe = false,
        isExplicit = false,
        bundleUrl = "",
        previewUrl = null,
        downloadsAmount = 0,
        knownIssues = emptyList(),
        publishedAt = LocalDateTime.now()
    ),
    availableVersion = null,
    contributors = listOf(
        Contributor(
            user = ContributorUserDto(
                id = "user_placeholder_id",
                username = "ContribuidorExemplo",
                imageUrl = null,
                createdAt = LocalDateTime.now()
            ),
            chartId = "placeholder_chart_id",
            roles = listOf(Role.AUDIO),
            joinedAt = LocalDateTime.now()
        ),
        Contributor(
            user = ContributorUserDto(
                id = "user_placeholder_id",
                username = "meumano2",
                imageUrl = null,
                createdAt = LocalDateTime.now()
            ),
            chartId = "placeholder_chart_id",
            roles = listOf(Role.AUDIO),
            joinedAt = LocalDateTime.now()
        ),
        Contributor(
            user = ContributorUserDto(
                id = "user_placeholder_id",
                username = "ala3alalalala3",
                imageUrl = null,
                createdAt = LocalDateTime.now()
            ),
            chartId = "placeholder_chart_id",
            roles = listOf(Role.AUDIO),
            joinedAt = LocalDateTime.now()
        )
    ),
)

val placeholderActivityItems = listOf(
    ActivityItem(
        date = Date(),
        content = listOf(placeholderChart, placeholderChart, placeholderChart)
    ),
    ActivityItem(
        date = Date(),
        content = listOf(placeholderChart, placeholderChart)
    )
)

val placeholderLibraryItems = listOf<CatalogItem>(
    placeholderChart,
    placeholderChart,
    placeholderChart,
    placeholderChart,
    placeholderChart,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    userId: String,
    user: User,
    onReturn: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val isOwner = userId == user.id
    val tabItems = if (isOwner) getOwnerTabItems() else getProfileTabItems()

    val horizontalPagerState = rememberPagerState { tabItems.size }

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
                        modifier = Modifier
                            .padding(end = 12.dp),
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
                        /*DropdownMenuUI {
                            // Add menu items here
                            DropdownMenuItem(
                                contentPadding = DropdownItemPadding,
                                text = { Text(stringResource(R.string.report)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_flag_24),
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    // Handle report action
                                }
                            )
                        }*/
                    }
                },
                title = {
                    Text("@${user.username}", style = MaterialTheme.typography.headlineMedium)
                },
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Banner
            item {
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

                    with(sharedTransitionScope) {
                        Avatar(
                            url = user.avatarUrl,
                            size = 96.dp,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .offset(x = 16.dp, y = (-16).dp)
                                .zIndex(1f)
                                .sharedElement(
                                    sharedTransitionScope.rememberSharedContentState(key = "profile_image"),
                                    animatedVisibilityScope = animatedContentScope
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
                                .sharedElement(
                                    sharedTransitionScope.rememberSharedContentState(key = "profile_icon"),
                                    animatedVisibilityScope = animatedContentScope
                                )
                                .graphicsLayer(
                                    alpha = 0f,
                                    scaleX = 0f,
                                    scaleY = 0f
                                )
                        )
                    }
                }
            }

            // Actions
            if (!isOwner) {
                item {
                    Row(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Button(
                            onClick = { /* Navigate to message user */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                modifier = Modifier.size(20.dp),
                                painter = painterResource(R.drawable.rounded_stars_24),
                                contentDescription = null
                            )
                            Text(modifier = Modifier.padding(start = 8.dp), text = "Follow")
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

            stickyHeader {
                PrimaryTabRow(
                    selectedTabIndex = horizontalPagerState.currentPage,
                    contentColor = MaterialTheme.colorScheme.onBackground,
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
                            modifier = Modifier.height(56.dp),
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

            // Recent activity
            item {
                HorizontalPager(
                    state = horizontalPagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(800.dp),
                    key = { it }, // Recompose the pager when the page changes
                    beyondViewportPageCount = 1 // Keep the next page in memory
                ) { index ->
                    when (index) {
                        0 -> if (isOwner) {
                            ProfileLikes(
                                items = placeholderLibraryItems,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            ProfileActivity(
                                items = placeholderActivityItems,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        1 -> if (isOwner) {
                            ProfileCollections(
                                items = placeholderLibraryItems,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            ProfileLibrary(
                                items = placeholderLibraryItems,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileActivity(items: List<ActivityItem>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.Top
    ) {
        items(items.size) { index ->
            val item = items[index]

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Timeline indicator
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top,
                    modifier = Modifier
                        .width(72.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .background(
                                MaterialTheme.colorScheme.onSecondaryContainer,
                                shape = RoundedCornerShape(99.dp)
                            )
                    )
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.onSecondaryContainer)
                    )
                }

                // Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        "@meninocoiso liked ${item.content.size} charts",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "5 days ago",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Chart previews
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        item.content.forEach { chart ->
                            when (chart) {
                                is Chart -> {
                                    ChartPreview(
                                        chart = chart,
                                        isSecondary = true,
                                        onPress = { /* Navigate to chart details */ }
                                    )
                                }

                                else -> { /* Handle other content types if necessary */
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileLibrary(items: List<CatalogItem>, modifier: Modifier = Modifier) {
    val chartsCount = items.filter { it is Chart }.size
    val tourPassesCount = items.filter { it is TourPass }.size
    val themesCount = items.filter { it is Theme }.size

    PullToRefreshBox(
        isRefreshing = false,
        onRefresh = { /*fetchUserLibrary()*/ }
    ) {
        LazyColumn(
            modifier = modifier,
            // verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ContentFilterUI(
                    options = listOf(
                        ContentFilterOption(
                            id = 0,
                            title = "All",
                            count = items.size
                        ),
                        ContentFilterOption(
                            id = 1,
                            title = "Collections",
                            count = 4
                        ),
                        ContentFilterOption(
                            id = 2,
                            title = "Charts",
                            count = chartsCount
                        ),
                        ContentFilterOption(
                            id = 3,
                            title = "Tour Passes",
                            count = tourPassesCount
                        ),
                        ContentFilterOption(
                            id = 4,
                            title = "Themes",
                            count = themesCount
                        )
                    ),
                    onClick = { /* Handle filter option click */ }
                )
            }
            items(items.size) { index ->
                when (val item = items[index]) {
                    is Chart -> {
                        ChartPreview(
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                            chart = item,
                            isSecondary = true,
                            onPress = { /* Navigate to chart details */ }
                        )
                    }

                    else -> { /* Handle other content types if necessary */
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileLikes(items: List<CatalogItem>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SegmentedButtonUI(options = listOf("Charts", "Tour Passes", "Themes"), onSelected = {})
        }
        items(items.size) { index ->
            when (val item = items[index]) {
                is Chart -> {
                    ChartPreview(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                        chart = item,
                        isSecondary = true,
                        onPress = { /* Navigate to chart details */ }
                    )
                }

                else -> { /* Handle other content types if necessary */
                }
            }
        }
    }
}

@Composable
fun ProfileCollections(items: List<CatalogItem>, modifier: Modifier = Modifier) {
    val chartsCount = items.filter { it is Chart }.size
    val tourPassesCount = items.filter { it is TourPass }.size
    val themesCount = items.filter { it is Theme }.size

    LazyColumn( modifier = modifier) {
        item {
            ContentFilterUI(
                options = listOf(
                    ContentFilterOption(
                        id = 0,
                        title = "All",
                        count = items.size
                    ),
                    ContentFilterOption(
                        id = 1,
                        title = "Collections",
                        count = 4
                    ),
                    ContentFilterOption(
                        id = 2,
                        title = "Charts",
                        count = chartsCount
                    ),
                    ContentFilterOption(
                        id = 3,
                        title = "Tour Passes",
                        count = tourPassesCount
                    ),
                    ContentFilterOption(
                        id = 4,
                        title = "Themes",
                        count = themesCount
                    )
                ),
                onClick = { /* Handle filter option click */ }
            )
        }
        items(items.size) { index ->
            when (val item = items[index]) {
                is Chart -> {
                    ChartPreview(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                        chart = item,
                        isSecondary = true,
                        onPress = { /* Navigate to chart details */ }
                    )
                }

                else -> { /* Handle other content types if necessary */
                }
            }
        }
    }
}

@Composable
fun BoxScope.ProfileIndicator(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(36.dp)
            .align(Alignment.BottomCenter)
            .zIndex(2f)
            .offset(y = 8.dp)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(100)
            )
            .clip(RoundedCornerShape(100))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(22.dp),
            painter = painterResource(R.drawable.rounded_person_24px),
            contentDescription = null
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    MaterialTheme {
        SharedTransitionLayout {
            AnimatedContent(
                targetState = true,
                label = "profile_preview"
            ) { _ ->
                ProfileScreen(
                    sharedTransitionScope = this@SharedTransitionLayout,
                    animatedContentScope = this@AnimatedContent,
                    userId = "preview_user_id",
                    user = User(
                        id = "preview_user_id",
                        username = "meninocoiso",
                        email = "user@example.com",
                        avatarUrl = null,
                        bannerUrl = null,
                        accentColor = 0xFF6200EE,
                        discordId = null,
                        createdAt = LocalDateTime.now()
                    ),
                    onReturn = {}
                )
            }
        }
    }
}

