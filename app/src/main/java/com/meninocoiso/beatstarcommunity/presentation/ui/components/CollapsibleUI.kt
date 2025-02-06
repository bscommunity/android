package com.meninocoiso.beatstarcommunity.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp

const val EXPANSION_ANIMATION_DURATION = 300

@Composable
fun CollapsableSection(
	title: String,
	initExpanded: Boolean? = true,
	content: @Composable () -> Unit
) {
	var isExpanded by remember {
		mutableStateOf(initExpanded ?: true)
	}

	val transition = updateTransition(targetState = isExpanded, label = "transition")
	val iconRotationDeg by transition.animateFloat(label = "iconRotation") {
		if (it) 180f else 0f
	}

	Column(
		modifier = Modifier
			.fillMaxWidth()
			.padding(horizontal = 16.dp)
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(vertical = 14.dp),
			horizontalArrangement = Arrangement.SpaceBetween
		) {
			Text(text = title, style = MaterialTheme.typography.titleMedium)
			Icon(
				imageVector = Icons.Default.KeyboardArrowDown,
				contentDescription = "Expand/Collapse icon",
				modifier = Modifier
					.rotate(iconRotationDeg)
					.clickable {
						isExpanded = !isExpanded
					}
			)
		}
		CollapsableSectionContent(content = content, isExpanded = isExpanded)
	}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CollapsableSectionContent(isExpanded: Boolean, content: @Composable () -> Unit) {
	val enterAnimation = remember {
		expandVertically(
			expandFrom = Alignment.Top,
			animationSpec = tween(
				durationMillis = EXPANSION_ANIMATION_DURATION
			)
		) + fadeIn(
			initialAlpha = .3f,
			animationSpec = tween(
				durationMillis = EXPANSION_ANIMATION_DURATION
			)
		)
	}

	val exitAnimation = remember {
		shrinkVertically(
			shrinkTowards = Alignment.Top,
			animationSpec = tween(
				durationMillis = EXPANSION_ANIMATION_DURATION
			)
		) + fadeOut(
			animationSpec = tween(
				durationMillis = EXPANSION_ANIMATION_DURATION
			)
		)
	}

	AnimatedVisibility(
		visible = isExpanded,
		enter = enterAnimation,
		exit = exitAnimation
	) {
		FlowRow(
			modifier = Modifier.padding(bottom = 16.dp),
			horizontalArrangement = Arrangement.spacedBy(8.dp),
			verticalArrangement = Arrangement.spacedBy(0.dp),
		) {
			content()
		}
	}
}