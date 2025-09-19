package com.meninocoiso.bscm.presentation.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
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
import com.meninocoiso.bscm.domain.enums.UpdatesSection
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.User
import com.meninocoiso.bscm.presentation.components.SwipeableSnackbarHost
import com.meninocoiso.bscm.presentation.screens.details.ChartDetails
import com.meninocoiso.bscm.presentation.screens.settings.Profile
import com.meninocoiso.bscm.presentation.screens.settings.SettingsScreen
import com.meninocoiso.bscm.presentation.screens.updates.UpdatesScreen
import com.meninocoiso.bscm.presentation.screens.workshop.WorkshopScreen
import com.meninocoiso.bscm.presentation.ui.components.layout.LaunchAppButton
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

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BottomNav(
    sharedTransitionScope: SharedTransitionScope,
    animatedContentScope: AnimatedContentScope,
    bottomNavController: NavHostController,
    navController: NavHostController,
    hasUpdate: Boolean = false,
    startOAuth: (Uri) -> Unit,
    cacheUser: User? = null,
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

    val onSnackbar: (String) -> Unit = { message ->
        coroutineScope.launch {
            println("BottomNav: Showing snackbar with message: $message")
            snackbarHostState.showSnackbar(message)
        }
    }

    val onNavigateToDetails = { chart: Chart ->
        navController.navigate(route = ChartDetails(chart = chart)) {
            // Prevent users from opening multiple details screens
            launchSingleTop = true
        }
    }
    
    val onNavigateToProfile = { user: User ->
        navController.navigate(route = Profile(user))
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
                    onNavigateToDetails,
                    onFabStateChange,
                    onSnackbar,
                )
            }
            composableWithFade<Route.Updates> { backStackEntry ->
                val updates: Route.Updates = backStackEntry.toRoute()
                UpdatesScreen(
                    updates.section,
                    onNavigateToDetails,
                    onSnackbar,
                    onFabStateChange
                )
            }
            composableWithFade<Route.Settings> {
                SettingsScreen(
                    sharedTransitionScope,
                    animatedContentScope,
                    startOAuth, 
                    cacheUser, 
                    onFabStateChange, 
                    onSnackbar, 
                    onNavigateToProfile)
            }
        }
    }
}