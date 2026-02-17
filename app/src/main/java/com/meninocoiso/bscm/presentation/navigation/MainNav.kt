package com.meninocoiso.bscm.presentation.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.meninocoiso.bscm.data.remote.dto.user.SimplifiedUser
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.Theme
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.domain.model.User
import com.meninocoiso.bscm.domain.serialization.ChartParameterType
import com.meninocoiso.bscm.domain.serialization.SimplifiedUserParameterType
import com.meninocoiso.bscm.presentation.screen.collection.Collection
import com.meninocoiso.bscm.presentation.screen.collection.CollectionScreen
import com.meninocoiso.bscm.presentation.screen.details.ChartDetails
import com.meninocoiso.bscm.presentation.screen.details.ChartDetailsRoute
import com.meninocoiso.bscm.presentation.screen.details.ChartDetailsScreen
import com.meninocoiso.bscm.presentation.screen.details.DeepLinkChartDetails
import com.meninocoiso.bscm.presentation.screen.profile.DeepLinkProfile
import com.meninocoiso.bscm.presentation.screen.profile.Profile
import com.meninocoiso.bscm.presentation.screen.profile.ProfileRoute
import com.meninocoiso.bscm.presentation.screen.profile.ProfileScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

@Serializable
object MainRoute

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainNav(startOAuth: (Uri) -> Unit, user: User?, hasUpdate: Boolean, intentFlow: Flow<Intent>) {
    val navController = rememberNavController()
    val bottomNavController = rememberNavController()

    val onNavigateToDetails = { item: CatalogItem ->
        when (item) {
            is Chart -> {
                // Navigate to chart details
                navController.navigate(route = ChartDetails(chart = item)) {
                    // Prevent users from opening multiple details screens
                    launchSingleTop = true
                }
            }

            else -> {
                // For unsupported types, open the web page as a fallback
                val url = when (item) {
                    is TourPass -> "https://bscm.dev/tourpass/${item.id}"
                    is Theme -> "https://bscm.dev/theme/${item.id}"
                }
                startOAuth(url.toUri())
            }
        }
    }

    val onNavigateToCollection = { collectionId: String ->
        navController.navigate(route = Collection(collectionId = collectionId)) {
            // Prevent users from opening multiple collection screens
            launchSingleTop = true
        }
    }

    val onNavigateToSettings = {
        bottomNavController.navigate(route = Route.Settings) {
            popUpTo(bottomNavController.graph.startDestinationId) {
                saveState = true
            }
            // Avoid multiple copies of the same destination when
            // reselecting the same item
            launchSingleTop = true

            // Restore cacheState when reselecting a previously selected item
            restoreState = true
        }
    }

    LaunchedEffect(Unit) {
        intentFlow.collect { intent ->
            // Handle deep links while the app is running
            navController.handleDeepLink(intent)
        }
    }

    // Wrap the SharedTransitionLayout with a Box that paints the background to avoid white flashes
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        SharedTransitionLayout {
            NavHost(
                navController = navController,
                startDestination = MainRoute,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Deep link to chart details
                composableWithTransitions<DeepLinkChartDetails>(
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "bscm://chart/{contentId}" }
                    )
                ) { backStackEntry ->
                    val chartDetails: DeepLinkChartDetails = backStackEntry.toRoute()
                    ChartDetailsRoute(
                        contentId = chartDetails.contentId,
                        onReturn = {
                            navController.navigateUp()
                        },
                        onNavigateToSettings = {
                            navController.navigateUp()
                            onNavigateToSettings()
                        }
                    )
                }

                // Deep link to user profile
                composableWithTransitions<DeepLinkProfile>(
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "bscm://profile/{username}" }
                    )
                ) { backStackEntry ->
                    val profileRoute: DeepLinkProfile = backStackEntry.toRoute()
                    ProfileRoute(
                        username = profileRoute.username,
                        onNavigateToDetails = { chart ->
                            onNavigateToDetails(chart)
                        },
                        onNavigateToCollection = { collectionId ->
                            onNavigateToCollection(collectionId)
                        },
                        onReturn = {
                            navController.navigateUp()
                        }
                    )
                }

                // Chart details
                composableWithTransitions<ChartDetails>(
                    typeMap = mapOf(
                        typeOf<Chart>() to ChartParameterType
                    )
                ) { backStackEntry ->
                    val chartDetails: ChartDetails = backStackEntry.toRoute()
                    ChartDetailsScreen(
                        chart = chartDetails.chart,
                        onReturn = {
                            navController.navigateUp()
                        },
                        onNavigateToSettings = {
                            navController.navigateUp()
                            onNavigateToSettings()
                        }
                    )
                }

                // Profile screen
                composable<Profile>(
                    typeMap = mapOf(
                        typeOf<SimplifiedUser>() to SimplifiedUserParameterType
                    )
                ) { backStackEntry ->
                    val profileRoute: Profile = backStackEntry.toRoute()
                    ProfileScreen(
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedContentScope = this,
                        user = profileRoute.user,
                        onReturn = {
                            navController.navigateUp()
                        },
                        onNavigateToDetails = { chart ->
                            onNavigateToDetails(chart)
                        },
                        onNavigateToCollection = { collectionId ->
                            onNavigateToCollection(collectionId)
                        }
                    )
                }

                // Collection screen
                composable<Collection> { backStackEntry ->
                    val collection: Collection = backStackEntry.toRoute()
                    CollectionScreen(
                        collectionId = collection.collectionId,
                        onNavigateToDetails = { chart ->
                            onNavigateToDetails(chart)
                        },
                        onReturn = {
                            navController.navigateUp()
                        }
                    )
                }

                composable<MainRoute> {
                    BottomNav(
                        this@SharedTransitionLayout,
                        this,
                        bottomNavController,
                        navController,
                        onNavigateToDetails,
                        onNavigateToSettings,
                        hasUpdate,
                        user,
                        startOAuth,
                    )
                }
            }
        }
    }
}