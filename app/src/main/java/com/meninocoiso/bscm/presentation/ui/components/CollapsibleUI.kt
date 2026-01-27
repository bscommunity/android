package com.meninocoiso.bscm.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.R

const val EXPANSION_ANIMATION_DURATION = 300

@Composable
fun CollapsableSection(
    modifier: Modifier = Modifier,
    initExpanded: Boolean? = true,
    header: @Composable (
        trigger: @Composable () -> Unit,
        interactionSource: MutableInteractionSource
    ) -> Unit,
    content: @Composable () -> Unit
) {
    var isExpanded by remember {
        mutableStateOf(initExpanded ?: true)
    }

    val transition = updateTransition(targetState = isExpanded, label = "transition")
    val iconRotationDeg by transition.animateFloat(label = "iconRotation") {
        if (it) 180f else 0f
    }

    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = {
                    isExpanded = !isExpanded
                },
                indication = null /*LocalIndication.current*/,
                interactionSource = interactionSource
            )
    ) {
        header({
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(R.string.expand_collapse),
                modifier = Modifier
                    .rotate(iconRotationDeg)
            )
        }, interactionSource)
        CollapsableSectionContent(content = content, isExpanded = isExpanded, modifier = modifier)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CollapsableSectionContent(
    isExpanded: Boolean,
    content: @Composable () -> Unit,
    modifier: Modifier
) {
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
            modifier = modifier.padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            content()
        }
    }
}