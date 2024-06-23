package com.meninocoiso.beatstarcommunity.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.meninocoiso.beatstarcommunity.data.classes.User

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ChartContributors(authors: List<User>) {
	var isExpanded by remember {
		mutableStateOf(false)
	}

	SharedTransitionLayout {
		AnimatedContent(
			isExpanded,
			label = "credits_transition"
		) { targetState ->
			if (!targetState) {
				CollapsedContributors(
					authors,
					onExpand = {
						isExpanded = true
					},
					animatedVisibilityScope = this@AnimatedContent,
					sharedTransitionScope = this@SharedTransitionLayout
				)
			} else {
				ExpandedContributors(
					authors,
					onCollapse = {
						isExpanded = false
					},
					animatedVisibilityScope = this@AnimatedContent,
					sharedTransitionScope = this@SharedTransitionLayout
				)
			}
		}
	}
}

@Composable
private fun Layout() {

}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CollapsedContributors(
	authors: List<User>,
	onExpand: () -> Unit,
	sharedTransitionScope: SharedTransitionScope,
	animatedVisibilityScope: AnimatedVisibilityScope
) {
	val authorsNames = authors.joinToString(", ") { "@${it.username}" }

	Box(
		modifier = Modifier
			.fillMaxWidth()
			.clickable() {
				onExpand()
			}
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(16.dp),
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			Row(
				horizontalArrangement = Arrangement.spacedBy(16.dp),
				verticalAlignment = Alignment.CenterVertically,
				modifier = Modifier
					//.background(Color.Green)
					.weight(1f) // This ensures that this Row takes the available space
			) {
				AuthorsAvatars(authors = authors, avatarSize = 48.dp)
				with(sharedTransitionScope) {
					Column {
						Text(
							modifier = Modifier.sharedElement(
								rememberSharedContentState(key = "credits-title"),
								animatedVisibilityScope = animatedVisibilityScope
							),
							text = "Credits",
							style = MaterialTheme.typography.titleMedium
						)
						Text(
							modifier = Modifier
								//.background(Color.Red),
								.sharedBounds(
									rememberSharedContentState(key = "credits-description"),
									animatedVisibilityScope = animatedVisibilityScope
								),
							text = "Chart by $authorsNames",
							overflow = TextOverflow.Ellipsis,
							maxLines = 1, // Limiting to 1 line
							style = MaterialTheme.typography.bodyMedium,
						)
					}
				}
			}
			with(sharedTransitionScope) {
				Icon(
					modifier = Modifier
						.width(48.dp)
					/*.sharedElement(
						rememberSharedContentState(key = "arrow-icon"),
						animatedVisibilityScope = animatedVisibilityScope
					)*/,
					imageVector = Icons.Default.KeyboardArrowDown,
					contentDescription = "Collapse/expand chart contributors"
				)
			}
		}
	}
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ExpandedContributors(
	authors: List<User>,
	onCollapse: () -> Unit,
	sharedTransitionScope: SharedTransitionScope,
	animatedVisibilityScope: AnimatedVisibilityScope
) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.background(Color.Green)
	) {
		Box(
			modifier = Modifier
				.fillMaxWidth()
				.clickable() {
					onCollapse()
				}
		) {
			Row(
				modifier = Modifier
					.fillMaxWidth()
					.padding(16.dp),
				horizontalArrangement = Arrangement.SpaceBetween,
				verticalAlignment = Alignment.CenterVertically,
			) {
				Row(
					horizontalArrangement = Arrangement.spacedBy(16.dp),
					verticalAlignment = Alignment.CenterVertically,
					modifier = Modifier
						//.background(Color.Green)
						.weight(1f) // This ensures that this Row takes the available space
				) {
					with(sharedTransitionScope) {
						Column {
							Text(
								modifier = Modifier.sharedElement(
									rememberSharedContentState(key = "credits-title"),
									animatedVisibilityScope = animatedVisibilityScope
								),
								text = "Credits",
								style = MaterialTheme.typography.titleMedium
							)
							Text(
								modifier = Modifier.sharedBounds(
									rememberSharedContentState(key = "credits-description"),
									animatedVisibilityScope = animatedVisibilityScope
								),
								text = "The following users contributed to this chart:",
								style = MaterialTheme.typography.bodyMedium,
							)
						}
					}
				}
				with(sharedTransitionScope) {
					Icon(
						modifier = Modifier
							.width(48.dp)
						/*.sharedElement(
							rememberSharedContentState(key = "arrow-icon"),
							animatedVisibilityScope = animatedVisibilityScope
						)*/,
						imageVector = Icons.Default.KeyboardArrowDown,
						contentDescription = "Collapse/expand chart contributors"
					)
				}
			}
		}
		LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp)) {
			for (author in authors) {
				item {
					Row(
						Modifier.padding(
							vertical = 8.dp
						),
						horizontalArrangement = Arrangement.spacedBy(16.dp),
						verticalAlignment = Alignment.CenterVertically
					) {
						Avatar(url = author.avatarUrl, size = 32.dp)
						Column {
							Text(
								text = "@${author.username}",
								style = MaterialTheme.typography.labelLarge
							)
							Text(
								text = "Test, test 2",
								style = MaterialTheme.typography.bodySmall,
							)
						}
					}
				}
			}
		}
	}
}
