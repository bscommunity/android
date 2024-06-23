package com.meninocoiso.beatstarcommunity.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.data.classes.Chart
import com.meninocoiso.beatstarcommunity.data.classes.User
import com.meninocoiso.beatstarcommunity.utils.DateUtils
import java.util.Date

@Composable
fun AuthorsAvatars(
	authors: List<User>,
	avatarSize: Dp? = null
) {
	Row(horizontalArrangement = Arrangement.spacedBy((-4).dp)) {
		for (author in authors) {
			if (avatarSize != null) {
				Avatar(url = author.avatarUrl, size = avatarSize)
			} else {
				Avatar(url = author.avatarUrl) // Use default size in Avatar
			}
		}
	}
}

@Composable
fun ChartAuthors(authors: List<User>, avatarSize: Dp? = null) {
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
			AuthorsAvatars(authors = authors, avatarSize = avatarSize)
			Text(
				style = MaterialTheme.typography.bodySmall,
				text = "Chart by ${authors[1].username}${if (authors.size > 2) " and ${authors.size - 2} more" else ""}"
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

@Composable
fun ChartPreview(
	modifier: Modifier = Modifier,
	chart: Chart,
	isFeatured: Boolean? = null,
	isRanked: Boolean? = null,
	isAdquired: Boolean? = null,
	onNavigateToDetails: () -> Unit = {}
) {
	val artistsNames = chart.song.artists.joinToString(", ") { it }

	Box(
		modifier = Modifier
			.fillMaxWidth()
			.clickable() {
				onNavigateToDetails()
			}
	) {
		Row(
			modifier = modifier
				.padding(16.dp)
				.fillMaxWidth(),
			horizontalArrangement = Arrangement.spacedBy(16.dp)
		) {
			CoverArt(difficulty = chart.difficulty, url = chart.song.coverArtUrl)
			Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
				Column {
					Row(
						modifier = Modifier.fillMaxWidth(),
						horizontalArrangement = Arrangement.SpaceBetween
					) {
						Text(
							text = chart.song.title,
							style = MaterialTheme.typography.titleMedium,
						)
						Text(
							style = MaterialTheme.typography.labelLarge,
							text = DateUtils.toRelativeString(chart.lastUpdatedAt)
						)
					}
					Text(style = MaterialTheme.typography.labelMedium, text = artistsNames)
				}
				ChartAuthors(authors = chart.authors)
				if (isAdquired == true) {
					Text(text = "Adquired")
				}
			}
		}
	}
}

@Composable
fun LocalChartPreview(
	modifier: Modifier = Modifier,
	chart: Chart,
	version: Int,
) {

}