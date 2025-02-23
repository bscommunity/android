package com.meninocoiso.beatstarcommunity.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.presentation.ui.components.CarouselUI
import com.meninocoiso.beatstarcommunity.presentation.ui.components.chart.ChartContributors
import com.meninocoiso.beatstarcommunity.presentation.ui.components.layout.Section
import com.meninocoiso.beatstarcommunity.util.DateUtils
import com.meninocoiso.beatstarcommunity.service.DownloadService.Companion.downloadChart
import kotlinx.serialization.Serializable

@Serializable
data class ChartDetails(val chart: Chart)

typealias OnNavigateToDetails = (chart: Chart) -> Unit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartDetailsScreen(
    chart: Chart,
    onReturn: () -> Unit
) {
    //println("ChartDetailsScreen: $chart")
    val lastVersion = chart.versions.last()

    val scrollState = rememberScrollState()
    var moreOptionsExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.padding(horizontal = 16.dp),
                navigationIcon = {
                    Icon(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clickable { onReturn() },
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = "Return"
                    )
                },
                actions = {
                    IconButton(onClick = { moreOptionsExpanded = !moreOptionsExpanded }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options menu"
                        )
                    }
                    DropdownMenu(
                        expanded = moreOptionsExpanded,
                        onDismissRequest = { moreOptionsExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share") },
                            leadingIcon = { Icon(Icons.Outlined.Share, contentDescription = null) },
                            onClick = { /* TODO */ }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete chart") },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) },
                            onClick = { /* TODO */ }
                        )
                    }
                },
                title = {
                    Column {
                        Text(chart.track, style = MaterialTheme.typography.titleLarge)
                        Text(chart.artist, style = MaterialTheme.typography.titleMedium)
                    }
                }
            )
        },
        bottomBar = {
            BottomAppBar(
                actions = {
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_artist_24),
                            contentDescription = "Track link"
                        )
                    }
                    IconButton(onClick = { /* TODO */ }) {
                        Icon(
                            Icons.Default.FavoriteBorder,
                            contentDescription = "Like chart",
                        )
                    }
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        text = { Text("Download") },
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.rounded_download_24),
                                contentDescription = ""
                            )
                        },
                        containerColor = BottomAppBarDefaults.bottomAppBarFabColor,
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation(),
                        onClick = { downloadChart(chart.id, lastVersion.chartUrl) }
                    )
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CarouselUI(listOf(chart.coverUrl))

            // Credits
            ChartContributors(chart.contributors)

            // Stats
            Section(
                title = "Stats"
            ) {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    StatListItem(
                        title = "~${DateUtils.toDurationString(lastVersion.duration)}",
                        icon = R.drawable.outline_access_time_24
                    )
                    StatListItem(
                        title = "${lastVersion.notesAmount} notes",
                        icon = R.drawable.rounded_music_note_24
                    )
                    StatListItem(
                        title = "+${lastVersion.notesAmount} downloads",
                        icon = R.drawable.rounded_download_24
                    )
                    StatListItem(
                        title = "Updated ${DateUtils.toRelativeString(lastVersion.publishedAt)}",
                        icon = R.drawable.rounded_calendar_today_24
                    )
                }
            }

            // Known Issues
            Section(
                title = "Known Issues"
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (lastVersion.knownIssues.isEmpty()) {
                            Text(
                                modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally),
                                text = "No known issues",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        } else {
                            lastVersion.knownIssues.forEach {
                                Text(
                                    text = "•   $it",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatListItem(
    title: String,
    icon: Int
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    //.background(Color.Red)
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = icon),
                    contentDescription = "Stat icon",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    )
}