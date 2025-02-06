package com.meninocoiso.beatstarcommunity.presentation.ui.components.chart

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.domain.model.User
import com.meninocoiso.beatstarcommunity.presentation.ui.components.layout.Avatar
import com.meninocoiso.beatstarcommunity.presentation.ui.components.layout.CoverArt
import com.meninocoiso.beatstarcommunity.util.DateUtils
import java.util.Date

@Composable
fun ChartAuthors(
	authors: List<User>,
	modifier: Modifier? = Modifier,
	avatarSize: Dp = 18.dp,
) {
	Box(
		modifier = Modifier.border(
			BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
			RoundedCornerShape(150.dp)
		)
	) {
		Row(
			modifier = Modifier
				//.background(Color.Blue)
				.padding(start = 6.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
			horizontalArrangement = Arrangement.spacedBy(8.dp),
			verticalAlignment = Alignment.CenterVertically
		) {
			Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
				for (author in authors) {
					Avatar(url = author.avatarUrl, size = avatarSize)
				}
			}
			Text(
				style = MaterialTheme.typography.bodySmall,
				text = "Chart by @${authors[0].username}${if (authors.size > 1) " and others" else ""}",
				maxLines = 1,
				overflow = TextOverflow.Ellipsis,
				modifier = (modifier ?: Modifier)
			)
		}
	}
}

@Preview
@Composable
fun ChartAuthorsPreview() {
	ChartAuthors(
		authors = listOf(
			User(
				"meninocoiso",
				"eduardomacielbr@gmail.com",
				"https://github.com/theduardomaciel.png",
				Date(),
				emptyList()
			)
		)
	)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChartPreview(
	chart: Chart,
	modifier: Modifier? = Modifier,
	isFeatured: Boolean? = null,
	isRanked: Boolean? = null,
	isAcquired: Boolean? = null,
	onNavigateToDetails: () -> Unit = {}
) {
	val artistsNames = chart.song.artists.joinToString(", ") { it }

	Box(
		modifier = (modifier ?: Modifier)
			.fillMaxWidth()
			.clickable() {
				onNavigateToDetails()
			}
	) {
		Row(
			modifier = Modifier
				.padding(16.dp)
				.fillMaxWidth(),
			horizontalArrangement = Arrangement.spacedBy(16.dp)
		) {
			CoverArt(difficulty = chart.difficulty, url = chart.song.coverArtUrl)
			Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
				Column {
					FlowRow(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween,
						verticalArrangement = Arrangement.Center,
					) {
						Text(
							text = chart.song.title,
							style = MaterialTheme.typography.titleMedium,
						)
						Text(
							style = MaterialTheme.typography.labelMedium,
							text = DateUtils.toRelativeString(chart.lastUpdatedAt)
						)
					}
					Text(style = MaterialTheme.typography.labelMedium, text = artistsNames)
				}
				ChartAuthors(authors = chart.authors, modifier = Modifier.fillMaxWidth(0.7f))
			}
		}
	}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocalChartPreview(
	chart: Chart,
	modifier: Modifier? = Modifier,
	version: Int,
	onNavigateToDetails: () -> Unit = {}
) {
	val artistsNames = chart.song.artists.joinToString(", ") { it }

	Box(
		modifier = (modifier ?: Modifier)
			.fillMaxWidth()
			.clip(RoundedCornerShape(16.dp))
			.clickable() {
				onNavigateToDetails()
			}
	) {
		Row(
			modifier = Modifier
				.background(MaterialTheme.colorScheme.surfaceContainerLow)
				.padding(16.dp)
				.fillMaxWidth(),
			horizontalArrangement = Arrangement.spacedBy(16.dp)
		) {
			CoverArt(
				difficulty = chart.difficulty,
				url = chart.song.coverArtUrl,
				borderRadius = 8.dp
			)
			Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
				Column {
					FlowRow(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween
					) {
						Text(
							text = chart.song.title,
							style = MaterialTheme.typography.titleMedium,
							maxLines = 1,
							overflow = TextOverflow.Ellipsis,
							modifier = Modifier.weight(1f)
						)
						Text(
							style = MaterialTheme.typography.labelLarge,
							text = "v$version"
						)
					}
					Text(style = MaterialTheme.typography.labelMedium, text = artistsNames)
				}
				ChartAuthors(authors = chart.authors)
			}
		}
	}
}