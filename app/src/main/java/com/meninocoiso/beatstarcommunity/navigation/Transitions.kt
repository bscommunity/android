package com.meninocoiso.beatstarcommunity.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

const val TRANSITION_DURATION = 350

inline fun <reified T : Any> NavGraphBuilder.composableWithTransitions(
	noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
	composable<T>(
		enterTransition = { enterTransition(this) },
		exitTransition = { exitTransition(this) },
		popEnterTransition = { popEnterTransition(this) },
		popExitTransition = { popExitTransition(this) },
		content = content
	)
}

fun enterTransition(anim: AnimatedContentTransitionScope<NavBackStackEntry>): EnterTransition {
	return anim.slideIntoContainer(
		AnimatedContentTransitionScope.SlideDirection.Left,
		animationSpec = tween(TRANSITION_DURATION)
	)
}

fun exitTransition(anim: AnimatedContentTransitionScope<NavBackStackEntry>): ExitTransition {
	return anim.slideOutOfContainer(
		AnimatedContentTransitionScope.SlideDirection.Left,
		animationSpec = tween(TRANSITION_DURATION)
	)
}

fun popEnterTransition(anim: AnimatedContentTransitionScope<NavBackStackEntry>): EnterTransition {
	return anim.slideIntoContainer(
		AnimatedContentTransitionScope.SlideDirection.Right,
		animationSpec = tween(TRANSITION_DURATION)
	)
}

fun popExitTransition(anim: AnimatedContentTransitionScope<NavBackStackEntry>): ExitTransition {
	return anim.slideOutOfContainer(
		AnimatedContentTransitionScope.SlideDirection.Right,
		animationSpec = tween(TRANSITION_DURATION)
	)
}
