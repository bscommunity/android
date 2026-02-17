package com.meninocoiso.bscm.presentation.ui.components.profile

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.remote.dto.user.SimplifiedUser
import com.meninocoiso.bscm.presentation.ui.components.layout.Avatar
import com.meninocoiso.bscm.presentation.ui.modifiers.roundedPolygonClip
import com.meninocoiso.bscm.presentation.ui.modifiers.roundedPolygonShape
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil3.CoilImage
import kotlinx.coroutines.launch

data class ProfileTabItem(val contentDescription: String, val iconResId: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSectionsLayout(
    user: SimplifiedUser,
    tabItems: List<ProfileTabItem>,
    onReturn: () -> Unit,
    topBarActions: @Composable RowScope.() -> Unit = {},
    headerActions: @Composable (() -> Unit)? = null,
    headerIdentity: @Composable BoxScope.() -> Unit,
    onTabSelected: (Int) -> Unit,
    pageContent: @Composable (Int) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val horizontalPagerState = rememberPagerState { tabItems.size }

    val headerScrollState = rememberScrollState()
    var headerHeightPx by remember { mutableFloatStateOf(0f) }
    val tabRowHeight = 56.dp

    LaunchedEffect(horizontalPagerState.currentPage) {
        onTabSelected(horizontalPagerState.currentPage)
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
                        onClick = onReturn
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            tint = MaterialTheme.colorScheme.onSurface,
                            contentDescription = stringResource(R.string.return_screen)
                        )
                    }
                },
                actions = topBarActions,
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
                        pageContent(index)
                    }
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(0, -headerScrollState.value) }
                        .onSizeChanged { size ->
                            headerHeightPx = size.height.toFloat()
                        }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ProfileBanner(user = user)
                        headerIdentity()
                    }

                    headerActions?.invoke()
                }

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

@Composable
private fun ProfileBanner(user: SimplifiedUser) {
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
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BoxScope.ProfileHeaderIdentity(
    user: SimplifiedUser,
    sharedTransitionScope: SharedTransitionScope? = null,
    animatedContentScope: AnimatedContentScope? = null,
) {
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
                } else {
                    Modifier
                }
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
                } else {
                    Modifier
                }
            )
            .graphicsLayer(
                alpha = 0f,
                scaleX = 0f,
                scaleY = 0f
            )
    )
}
