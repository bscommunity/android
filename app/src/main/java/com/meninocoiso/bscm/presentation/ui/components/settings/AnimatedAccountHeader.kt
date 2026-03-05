package com.meninocoiso.bscm.presentation.ui.components.settings

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meninocoiso.bscm.data.remote.dto.user.SimplifiedUser
import com.meninocoiso.bscm.presentation.screen.profile.OnNavigateToProfile
import com.meninocoiso.bscm.presentation.ui.components.layout.Avatar
import com.meninocoiso.bscm.presentation.ui.components.profile.ProfileIndicator
import com.meninocoiso.bscm.presentation.ui.modifiers.roundedPolygonClip
import com.meninocoiso.bscm.presentation.ui.modifiers.roundedPolygonShape

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AnimatedAccountHeader(
    user: SimplifiedUser,
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    onNavigateToProfile: OnNavigateToProfile
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.padding(vertical = 24.dp)
    ) {
        with(sharedTransitionScope) {
            Avatar(
                url = user.avatarUrl,
                size = 128.dp,
                modifier = Modifier
                    .sharedElement(
                        sharedTransitionScope.rememberSharedContentState(key = "profile_image"),
                        animatedContentScope
                    )
                    .border(
                        width = 0.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = roundedPolygonShape()
                    )
                    .roundedPolygonClip()
                    .clickable(
                        onClick = {
                            onNavigateToProfile(user)
                        },
                        indication = ripple(
                            bounded = true,
                            radius = Dp.Unspecified,
                            color = Color.Black
                        ),
                        interactionSource = remember { MutableInteractionSource() }
                    )
            )
            ProfileIndicator(
                modifier = Modifier
                    .sharedElement(
                        sharedTransitionScope.rememberSharedContentState(key = "profile_icon"),
                        animatedContentScope
                    )
                    .graphicsLayer(
                        alpha = 1f,
                        scaleX = 1f,
                        scaleY = 1f
                    )
            )
        }
    }
}