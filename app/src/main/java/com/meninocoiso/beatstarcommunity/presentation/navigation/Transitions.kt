package com.meninocoiso.beatstarcommunity.presentation.navigation

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import kotlin.reflect.KType

const val FADE_DURATION = 200
const val TRANSITION_DURATION = 350

inline fun <reified T : Any> NavGraphBuilder.composableWithFade(
	noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
	composable<T>(
		enterTransition = {
			fadeIn(
				animationSpec = tween(FADE_DURATION)
			)
		},
		exitTransition = {
			fadeOut(
				animationSpec = tween(FADE_DURATION)
			)
		},
		popEnterTransition = {
			fadeIn(
				animationSpec = tween(FADE_DURATION)
			)
		},
		popExitTransition = {
			fadeOut(
				animationSpec = tween(FADE_DURATION)
			)
		},
		content = content
	)
}

inline fun <reified T : Any> NavGraphBuilder.composableWithTransitions(
	typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
	noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit,
	) {
	composable<T>(
		typeMap = typeMap,
		enterTransition = { enterTransition(this) },
		exitTransition = { exitTransition(this) },
		popEnterTransition = { popEnterTransition(this) },
		popExitTransition = { popExitTransition(this) },
		content = content
	)
}

inline fun <reified T : Any> NavGraphBuilder.composableWithoutTransitions(
	noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
	composable<T>(
		enterTransition = { EnterTransition.None },
		exitTransition = { ExitTransition.None },
		popEnterTransition = { popEnterTransition(this) },
		popExitTransition = { popExitTransition(this) },
		content = content
	)
}

fun enterTransition(anim: AnimatedContentTransitionScope<NavBackStackEntry>): EnterTransition {
	return slideInHorizontally(initialOffsetX = { it }) + fadeIn()
}

fun exitTransition(anim: AnimatedContentTransitionScope<NavBackStackEntry>): ExitTransition {
	return slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
}

fun popEnterTransition(anim: AnimatedContentTransitionScope<NavBackStackEntry>): EnterTransition {
	return slideInHorizontally(initialOffsetX = { -it })
}

fun popExitTransition(anim: AnimatedContentTransitionScope<NavBackStackEntry>): ExitTransition {
	return slideOutHorizontally(targetOffsetX = { it })
}
