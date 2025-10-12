package com.meninocoiso.bscm.presentation.screen.settings

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.domain.model.User
import com.meninocoiso.bscm.presentation.screen.details.DropdownItemPadding
import com.meninocoiso.bscm.presentation.ui.components.DropdownMenuUI
import com.meninocoiso.bscm.presentation.ui.components.layout.Avatar
import com.meninocoiso.bscm.presentation.ui.modifiers.roundedPolygonClip
import com.meninocoiso.bscm.presentation.ui.modifiers.roundedPolygonShape
import com.meninocoiso.bscm.presentation.viewmodel.ProfileViewModel
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil3.CoilImage
import kotlinx.serialization.Serializable

@Serializable
data class Profile(val user: User)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileScreen(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    user: User,
    onReturn: () -> Unit,
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            MediumTopAppBar(
                modifier = Modifier.padding(horizontal = 8.dp),
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
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.share)
                        )
                    }
                    DropdownMenuUI {
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
                    }
                },
                title = {
                    Text("@meninocoiso", style = MaterialTheme.typography.headlineMedium)
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
                    when (null) {
                        null -> Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .size(180.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .zIndex(1f)
                        )

                        else -> CoilImage(
                            imageModel = { user.imageUrl },
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
                            url = user.imageUrl,
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

            // Stats
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProfileStatItem(
                        icon = R.drawable.rounded_history_24,
                        label = "Member since June 2023",
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp)
                    )
                    ProfileStatDivider()
                    ProfileStatItem(
                        icon = R.drawable.rounded_favorite_24,
                        label = "+500 liked charts",
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp)
                    )
                    ProfileStatDivider()
                    ProfileStatItem(
                        icon = R.drawable.rounded_bookmark_24,
                        label = "+20 favorite charts",
                        modifier = Modifier
                            .weight(1f)
                            .padding(16.dp)
                    )
                }
            }

            // Recent activity
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = { /* Navigate to recent activity */ })
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Recent activity",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null
                    )
                }
            }

            // List of recent activity
            /*items(10) {
                ChartPreview(
                    Chart(
                        id = "placeholder_id",
                        shareId = "share_placeholder",
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
                        contributors = emptyList(),
                    ),
                    onNavigateToDetails = { *//* Navigate to chart details *//* }
                )
            }*/
        }
    }
}

@Composable
fun ProfileStatItem(
    icon: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
fun ProfileStatDivider() {
    Box(
        modifier = Modifier
            .padding(vertical = 8.dp)
            .size(width = 1.dp, height = 36.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
    )
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