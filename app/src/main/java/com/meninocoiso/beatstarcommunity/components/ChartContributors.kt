package com.meninocoiso.beatstarcommunity.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CollapsedContributors(
	authors: List<User>,
	onExpand: () -> Unit,
	modifier: Modifier = Modifier,
	sharedTransitionScope: SharedTransitionScope,
	animatedVisibilityScope: AnimatedVisibilityScope
) {
	val authorsNames = authors.joinToString(", ") { "@${it.username}" }

	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(16.dp)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.clickable {
					onExpand()
				},
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically
		) {
			Row(
				horizontalArrangement = Arrangement.spacedBy(16.dp),
				verticalAlignment = Alignment.CenterVertically
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
							modifier = Modifier.sharedBounds(
								rememberSharedContentState(key = "credits-description"),
								animatedVisibilityScope = animatedVisibilityScope
							),
							text = "Chart by $authorsNames",
							overflow = TextOverflow.Ellipsis,
							style = MaterialTheme.typography.bodyMedium,
						)
					}
				}
			}
			Icon(
				imageVector = Icons.Default.KeyboardArrowDown,
				contentDescription = "Collapse/expand chart contributors"
			)
		}
	}
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ExpandedContributors(
	authors: List<User>,
	onCollapse: () -> Unit,
	modifier: Modifier = Modifier,
	sharedTransitionScope: SharedTransitionScope,
	animatedVisibilityScope: AnimatedVisibilityScope
) {
	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(16.dp),
		verticalArrangement = Arrangement.spacedBy(8.dp)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.clickable {
					onCollapse()
				},
			horizontalArrangement = Arrangement.SpaceBetween,
			verticalAlignment = Alignment.CenterVertically,
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
			IconButton(onClick = onCollapse) {
				Icon(
					imageVector = Icons.Default.KeyboardArrowDown,
					contentDescription = "Collapse/expand chart contributors"
				)
			}
		}
		LazyColumn {
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
