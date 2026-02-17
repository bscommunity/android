package com.meninocoiso.bscm.presentation.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.meninocoiso.bscm.R
import com.meninocoiso.bscm.data.remote.dto.user.SimplifiedUser
import com.meninocoiso.bscm.domain.enums.UpdatesSection
import com.meninocoiso.bscm.domain.model.User
import com.meninocoiso.bscm.presentation.screen.details.OnNavigateToDetails
import com.meninocoiso.bscm.presentation.screen.profile.Profile
import com.meninocoiso.bscm.presentation.screen.settings.SettingsScreen
import com.meninocoiso.bscm.presentation.screen.updates.UpdatesScreen
import com.meninocoiso.bscm.presentation.screen.workshop.WorkshopScreen
import com.meninocoiso.bscm.presentation.ui.components.layout.LaunchAppButton
import com.meninocoiso.bscm.presentation.ui.components.layout.SwipeableSnackbarHost
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable
sealed class Route {
    @Serializable
    object Workshop : Route()

    @Serializable
    data class Updates(val section: UpdatesSection = UpdatesSection.Workshop) : Route()

    @Serializable
    object Settings : Route()
}

@Composable
fun getBottomNavigationItems(): List<BottomNavigationItem> {
    return listOf(
        BottomNavigationItem(
            route = Route.Workshop,
            title = stringResource(R.string.workshop),
            selectedIcon = R.drawable.baseline_library_music_24,
            unselectedIcon = R.drawable.outline_library_music_24,
            hasNews = false
        ),
        BottomNavigationItem(
            // NOTE: If you use routes with arguments as your BottomBar navigation routes, 
            // // first of all, you should instantiate such classes Route.Updates():
            route = Route.Updates(section = UpdatesSection.Workshop),
            title = stringResource(R.string.updates),
            selectedIcon = R.drawable.baseline_deployed_code_24,
            unselectedIcon = R.drawable.outline_deployed_code_24,
            hasNews = false
        ),
        BottomNavigationItem(
            route = Route.Settings,
            title = stringResource(R.string.settings),
            selectedIcon = R.drawable.baseline_settings_24,
            unselectedIcon = R.drawable.outline_settings_24,
            hasNews = false
        )
    )
}

typealias OnSnackbar = (
    message: String,
    actionLabel: String?,
    withDismissAction: Boolean,
    duration: SnackbarDuration,
    onAction: (() -> Unit)?,
        onDismiss: (() -> Unit)?
) -> Unit

fun OnSnackbar.show(
    message: String,
    actionLabel: String? = null,
    withDismissAction: Boolean = false,
    duration: SnackbarDuration =
        if (actionLabel == null) SnackbarDuration.Short else SnackbarDuration.Indefinite,
    onAction: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) = this(message, actionLabel, withDismissAction, duration, onAction, onDismiss)


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BottomNav(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    bottomNavController: NavHostController,
    navController: NavHostController,
    onNavigateToDetails: OnNavigateToDetails,
    onNavigateToSettings: () -> Unit,
    hasUpdate: Boolean = false,
    user: User?,
    startOAuth: (Uri) -> Unit,
) {
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()

    val bottomNavigationItems = getBottomNavigationItems()

    // We update Settings item icon based on the update status
    val updatedBottomNavigationItems = remember(bottomNavigationItems, hasUpdate) {
        bottomNavigationItems.map { item ->
            if (item.route is Route.Settings) {
                item.copy(hasNews = hasUpdate) // Update the badge dynamically
            } else {
                item
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var fabExtended by remember { mutableStateOf(true) }

    val onSnackbar: OnSnackbar = { message, actionLabel, withDismissAction, duration, onAction, onDismiss ->
        coroutineScope.launch {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = actionLabel,
                withDismissAction = withDismissAction,
                duration = duration
            )
            
            when (result) {
                SnackbarResult.Dismissed -> {
                    onDismiss?.invoke()
                }
                SnackbarResult.ActionPerformed -> {
                    onAction?.invoke()
                }
            }
        }
    }

    val onNavigateToProfile = { user: SimplifiedUser ->
        navController.navigate(route = Profile(
            user = user,
        ))
    }

    val onFabStateChange: (Boolean) -> Unit = { shouldExtend ->
        if (shouldExtend != fabExtended) {
            fabExtended = shouldExtend
        }
    }

    Scaffold(
        snackbarHost = { SwipeableSnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            BottomNavBar(
                navBackStackEntry = navBackStackEntry,
                onClick = { route ->
                    bottomNavController.navigate(route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        popUpTo(bottomNavController.graph.startDestinationId) {
                            saveState = true
                        }

                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true

                        // Restore cacheState when reselecting a previously selected item
                        restoreState = true
                    }
                },
                bottomNavigationItems = updatedBottomNavigationItems,
            )
        },
        floatingActionButton = {
            LaunchAppButton(
                onNavigateToUpdates = {
                    bottomNavController.navigate(
                        route = Route.Updates(section = UpdatesSection.Installations)
                    ) {
                        popUpTo(bottomNavController.graph.startDestinationId) {
                            saveState = true
                        }
                    }
                },
                extended = fabExtended,
                /*modifier = Modifier.graphicsLayer {
                    translationY = fabOffset
                }*/
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        NavHost(
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
            navController = bottomNavController,
            startDestination = Route.Workshop,
        ) {
            composableWithFade<Route.Workshop> {
                WorkshopScreen(
                    onSnackbar = onSnackbar,
                    onFabStateChange = onFabStateChange,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToDetails = onNavigateToDetails,
                )
            }
            composableWithFade<Route.Updates> { backStackEntry ->
                val updates: Route.Updates = backStackEntry.toRoute()
                UpdatesScreen(
                    section = updates.section,
                    onNavigateToDetails = onNavigateToDetails,
                    onSnackbar = onSnackbar,
                    onFabStateChange = onFabStateChange
                )
            }
            composableWithFade<Route.Settings> {
                SettingsScreen(
                    sharedTransitionScope = sharedTransitionScope,
                    animatedContentScope = animatedContentScope,
                    user = user,
                    startOAuth = startOAuth,
                    onFabStateChange = onFabStateChange,
                    onSnackbar = onSnackbar,
                    onNavigateToProfile = onNavigateToProfile
                )
            }
        }
    }
}