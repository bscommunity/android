package com.meninocoiso.beatstarcommunity.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.R
import com.meninocoiso.beatstarcommunity.presentation.ui.components.CarouselUI
import com.meninocoiso.beatstarcommunity.presentation.ui.components.layout.Section
import kotlinx.serialization.Serializable

@Serializable
object ChartDetails

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartDetailsScreen(
	onReturn: () -> Unit
) {
	val scrollState = rememberScrollState()

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
					Icon(
						imageVector = Icons.Outlined.FavoriteBorder,
						contentDescription = "Like content"
					)
				},
				title = {
					Column {
						Text("We Live Forever", style = MaterialTheme.typography.titleLarge)
						Text("The Prodigy", style = MaterialTheme.typography.titleMedium)
					}
				}
			)
		},
		bottomBar = {
			BottomAppBar(
				actions = {
					IconButton(onClick = { /* TODO */ }) {
						Icon(
							Icons.Filled.MoreVert,
							contentDescription = "More options"
						)
					}
					IconButton(onClick = { /* TODO */ }) {
						Icon(
							Icons.Outlined.Delete,
							contentDescription = "Delete chart",
						)
					}
					IconButton(onClick = { /* TODO */ }) {
						Icon(
							Icons.Outlined.Share,
							contentDescription = "Share chart",
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
						onClick = {
							/* TODO */
						},
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
			/*CarouselUI(imageUrls)*/

			// Credits
			/*ChartContributors(
				listOf(
					User(
						username = "meninocoiso",
						email = "teste@gmail.com",
						imageUrl = "https://github.com/theduardomaciel.png",
						createdAt = Date(),
						charts = null
					),
					User(
						username = "oCosmo55",
						email = "teste@gmail.com",
						imageUrl = "https://github.com/oCosmo55.png",
						createdAt = Date(),
					),
					User(
						username = "extreme",
						email = "teste@gmail.com",
						imageUrl = "https://github.com/theduardomaciel.png",
						createdAt = Date(),
						charts = null
					),
				)
			)*/

			// Stats
			Section(
				title = "Stats"
			) {
				Column(modifier = Modifier.padding(bottom = 8.dp)) {
					/*StatListItem(
						title = "~${chart.song.duration} minutes",
						icon = R.drawable.outline_access_time_24
					)
					StatListItem(
						title = "${chart.notesAmount} notes",
						icon = R.drawable.rounded_music_note_24
					)
					StatListItem(
						title = "+${chart.notesAmount} downloads",
						icon = R.drawable.rounded_download_24
					)
					StatListItem(
						title = "Updated ${DateUtils.toRelativeString(chart.lastUpdatedAt)}",
						icon = R.drawable.rounded_calendar_today_24
					)*/
				}
			}

			// Known Issues
			Section(
				title = "Known Issues"
			) {
				/*if (chart.knownIssues?.isEmpty() == true) {
					Text(text = "No known issues", style = MaterialTheme.typography.bodyLarge)
				} else {
					Box(modifier = Modifier.padding(16.dp)) {
						Column(
							modifier = Modifier
								.fillMaxWidth()
								.padding(start = 16.dp),
							horizontalAlignment = Alignment.Start,
							verticalArrangement = Arrangement.spacedBy(8.dp),
						) {
							chart.knownIssues?.forEach {
								Text(text = "•   $it", style = MaterialTheme.typography.bodyLarge)
							}
						}
					}
				}*/
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