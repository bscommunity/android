package com.meninocoiso.beatstarcommunity.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.data.remote.dto.ContributorUserDto
import com.meninocoiso.beatstarcommunity.domain.enums.ContributorRoleEnum
import com.meninocoiso.beatstarcommunity.domain.enums.DifficultyEnum
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.domain.model.Contributor
import com.meninocoiso.beatstarcommunity.domain.model.StreamingLink
import com.meninocoiso.beatstarcommunity.domain.model.Version
import com.meninocoiso.beatstarcommunity.presentation.ui.components.StatusMessageUI
import com.meninocoiso.beatstarcommunity.presentation.ui.components.chart.ChartPreview
import com.meninocoiso.beatstarcommunity.presentation.ui.components.workspace.WorkspaceChips
import com.meninocoiso.beatstarcommunity.presentation.ui.components.workspace.WorkspaceTopBar
import com.meninocoiso.beatstarcommunity.presentation.ui.components.workspace.workspaceTabsItems
import com.meninocoiso.beatstarcommunity.presentation.ui.modifiers.fabScrollObserver
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.ChartViewModel
import com.meninocoiso.beatstarcommunity.presentation.viewmodel.ChartsState
import com.meninocoiso.beatstarcommunity.util.AppBarUtils
import java.time.LocalDate

private val SearchBarHeight = 80.dp
private val TabsHeight = 48.dp
private val WorkspaceChipsHeight = 56.dp

@Composable
fun WorkspaceScreen(
    onNavigateToDetails: OnNavigateToDetails,
    onFabStateChange: (Boolean) -> Unit
) {
    val horizontalPagerState = rememberPagerState {
        workspaceTabsItems.size
    }

    val (connection, spaceHeight, statusBarHeight) = AppBarUtils.getConnection(
        collapsableHeight = SearchBarHeight,
        fixedHeight = TabsHeight,
        bottomCollapsableHeight = WorkspaceChipsHeight,
    )

    Column {
        Column(
            Modifier
                .height(spaceHeight),
            verticalArrangement = Arrangement.Bottom,
        ) {
            WorkspaceChips(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(5f)
            )
        }

        HorizontalPager(
            state = horizontalPagerState,
            key = { it }, // Recompose the pager when the page changes
            beyondViewportPageCount = 1 // Keep the next page in memory
        ) { index ->
            when (
                index
            ) {
                0 -> ChartsSection(
                    connection,
                    onNavigateToDetails,
                    onFabStateChange
                )

                1 -> TourPassesSection(connection)
                2 -> ThemesSection(connection)
            }
        }
    }

    WorkspaceTopBar(
        connection = connection,
        appBarHeights = Triple(SearchBarHeight, TabsHeight, statusBarHeight),
        pagerState = horizontalPagerState,
    )
}

@Composable
private fun SectionWrapper(
    nestedScrollConnection: NestedScrollConnection,
    onFabStateChange: (Boolean) -> Unit,
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .fabScrollObserver { shouldExtend ->
                // Update FAB state based on scroll delta
                onFabStateChange(shouldExtend)
            },
    ) {
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsSection(
    nestedScrollConnection: NestedScrollConnection,
    onNavigateToDetails: OnNavigateToDetails,
    onFabStateChange: (Boolean) -> Unit,
    viewModel: ChartViewModel = hiltViewModel()
) {
    val chartsState by viewModel.charts.collectAsState()
    /*val (cachedCharts, setCachedCharts) = remember { mutableStateOf(emptyList<Chart>()) }*/

    val data = LocalDate.now()

    val cachedCharts = listOf(
        Chart(
            id = "8d3d2eb4-5218-48af-ae33-a73413889353",
            artist = "SOCKiTTOME",
            track = "mutual",
            album = "mutual - Single",
            coverUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music221/v4/b5/df/24/b5df24cf-9c74-1611-be18-564e57710407/2b958736-d490-4f69-9e53-fbba2d61c53b.jpg/600x600bb.jpg",
            difficulty = DifficultyEnum.Hard,
            isDeluxe = true,
            isExplicit = false,
            isFeatured = false,
            isInstalled = false,
            trackUrls = listOf(
                StreamingLink(
                    platform = "Youtube Music",
                    url = "https://music.youtube.com/watch?v=1n19mhKWoLk&si=5aoJ04_2MgDD2vHl"
                ), StreamingLink(
                    platform = "Spotify",
                    url = "https://open.spotify.com/intl-pt/track/17JZksbLTp4IINaVdu0whZ?si=4a8ee878174348f2"
                ), StreamingLink(
                    platform = "Apple Music",
                    url = "https://music.apple.com/us/album/chihiro/1739659134?i=1739659141&uo=4"
                ), StreamingLink(
                    platform = "Deezer",
                    url = "https://www.deezer.com/br/track/2801558042"
                ), StreamingLink(
                    platform = "Tidal",
                    url = "https://tidal.com/browse/video/367440468"
                )
            ),
            trackPreviewUrl = "a",
            latestVersion = Version(
                id = "8d3d2eb4-5218-48af-ae33-a7341388a353",
                index = 1,
                chartId = "8d3d2eb4-5218-48af-ae33-a73413889353",
                duration = 21.25f,
                notesAmount = 16,
                effectsAmount = 3,
                bpm = 120,
                chartUrl = "https://cdn.discordapp.com/attachments/954166390619783268/1343421514770415727/HOT_TO_GO.zip?ex=67c86b08&is=67c71988&hm=74388cc07990dcb67e9efabecfee62b0149a70efe3f3619031712a20b6e4b45e&",
                downloadsAmount = 0,
                knownIssues = listOf(),
                publishedAt = data
            ),
            contributors = listOf(
                Contributor(
                    user = ContributorUserDto(
                        id = "4f504276-17de-48b4-84a6-fc5750d957cc",
                        username = "bscommunity",
                        imageUrl = null,
                        createdAt = null
                    ),
                    chartId = "8d3d2eb4-5218-48af-ae33-a73413889353",
                    roles = listOf(ContributorRoleEnum.Author),
                    joinedAt = data
                )
            )
        )
    )

    val snackbarHostState = remember { SnackbarHostState() }

    /*LaunchedEffect(chartsState) {
        when (chartsState) {
            is ChartsState.Success -> {
                setCachedCharts((chartsState as ChartsState.Success).charts)
            }
            is ChartsState.Error -> {
                *//*val errorMsg = (chartsState as ChartsState.Error).message ?: "Unknown error"*//*
				if (cachedCharts.isNotEmpty()) {
					snackbarHostState.showSnackbar(
						"Failed to fetch new data. Please check your connection and try again",
					)
				}
			}
			else -> {}
		}
	}*/

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            when {
                (cachedCharts.isEmpty() && chartsState is ChartsState.Loading) -> {
                    // Show spinner if no cached data
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(Modifier.width(36.dp))
                    }
                }

                cachedCharts.isNotEmpty() -> {
                    // Show cached data and pull-to-refresh
                    PullToRefreshBox(
                        isRefreshing = chartsState is ChartsState.Loading,
                        onRefresh = { viewModel.refresh() }
                    ) {
                        SectionWrapper(
                            nestedScrollConnection,
                            onFabStateChange
                        ) {
                            items(cachedCharts.flatMap { chart -> List(20) { chart } }) { chart ->
                                ChartPreview(
                                    onNavigateToDetails = { onNavigateToDetails(chart) },
                                    chart = chart
                                )
                            }
                        }
                    }
                }

                chartsState is ChartsState.Error -> {
                    // Display error with a retry option
                    StatusMessageUI(
                        title = "Looks like something went wrong...",
                        message = "Please check your connection and try again",
                        icon = R.drawable.rounded_emergency_home_24,
                        onClick = { viewModel.refresh() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
private fun TourPassesSection(nestedScrollConnection: NestedScrollConnection) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        StatusMessageUI(
            title = "Work in progress!",
            message = "This feature still needs some work\nPlease, check back later",
            icon = R.drawable.rounded_hourglass_24,
        )
    }
}

@Composable
private fun ThemesSection(nestedScrollConnection: NestedScrollConnection) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        StatusMessageUI(
            title = "Work in progress!",
            message = "This feature still needs some work\nPlease, check back later",
            icon = R.drawable.rounded_hourglass_24,
        )
    }
}