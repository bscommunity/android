package com.meninocoiso.bscm.presentation.ui.components.chart

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.domain.model.Contributor
import com.meninocoiso.bscm.presentation.ui.components.layout.Avatar
import com.meninocoiso.bscm.util.StringUtils

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ChartContributors(authors: List<Contributor>) {
    var isExpanded by remember {
        mutableStateOf(false)
    }

    val transition = updateTransition(targetState = isExpanded, label = "transition")
    val iconRotationDeg by transition.animateFloat(label = "iconRotation") {
        if (it) 180f else 0f
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
                    iconRotationDeg = iconRotationDeg,
                    animatedVisibilityScope = this@AnimatedContent,
                    sharedTransitionScope = this@SharedTransitionLayout
                )
            } else {
                ExpandedContributors(
                    authors,
                    onCollapse = {
                        isExpanded = false
                    },
                    iconRotationDeg = iconRotationDeg,
                    animatedVisibilityScope = this@AnimatedContent,
                    sharedTransitionScope = this@SharedTransitionLayout
                )
            }
        }
    }
}

@Composable
private fun Layout(
    onEvent: () -> Unit,
    iconRotationDeg: Float,
    header: @Composable RowScope.() -> Unit,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
        //.background(Color.Green)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onEvent()
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                header(this)
                Icon(
                    modifier = Modifier
                        .width(48.dp)
                        .rotate(iconRotationDeg),
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Collapse/expand chart contributors"
                )
            }
        }
        content()
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun CollapsedContributors(
    authors: List<Contributor>,
    onExpand: () -> Unit,
    iconRotationDeg: Float,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    val users = authors.map(Contributor::user)
    val authorsNames = users.joinToString(", ") { "@${it.username}" }

    Layout(
        onEvent = onExpand,
        iconRotationDeg,
        header = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    //.background(Color.Blue)
                    .weight(1f) // How to apply it here without import errors
            ) {
                with(sharedTransitionScope) {
                    Row(horizontalArrangement = Arrangement.spacedBy((-16).dp)) {
                        for (author in users) {
                            // println("id from collapsed: ${author.username} ${author.id}")
                            Avatar(
                                modifier = Modifier.sharedElement(
                                    rememberSharedContentState(key = author.id),
                                    animatedVisibilityScope = animatedVisibilityScope
                                ),
                                // key = "avatar-${author.username}",
                                url = author.imageUrl,
                                alt = author.username.first().toString(),
                                size = 48.dp
                            )
                        }
                    }
                    Column {
                        Text(
                            modifier = Modifier.sharedBounds(
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
        }) {
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun ExpandedContributors(
    authors: List<Contributor>,
    onCollapse: () -> Unit,
    iconRotationDeg: Float,
    sharedTransitionScope: SharedTransitionScope,
    animatedVisibilityScope: AnimatedVisibilityScope
) {
    Layout(
        onEvent = onCollapse,
        iconRotationDeg,
        header = {
            with(sharedTransitionScope) {
                Column {
                    Text(
                        modifier = Modifier.sharedBounds(
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
        }) {
        Column(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            for (author in authors) {
                Row(
                    Modifier.padding(
                        vertical = 8.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // println("id from expanded: ${author.user.username} ${author.user.id}")
                    with(sharedTransitionScope) {
                        Avatar(
                            url = author.user.imageUrl,
                            alt = author.user.username.first().toString(),
                            // key = "avatar-${author.user.username}",
                            size = 32.dp,
                            modifier = Modifier.sharedElement(
                                rememberSharedContentState(key = author.user.id),
                                animatedVisibilityScope = animatedVisibilityScope
                            )
                        )
                    }
                    Column {
                        Text(
                            text = "@${author.user.username}",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = author.roles.joinToString(", ") {
                                StringUtils.getAuthorRole(it)
                            },
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
    }
}