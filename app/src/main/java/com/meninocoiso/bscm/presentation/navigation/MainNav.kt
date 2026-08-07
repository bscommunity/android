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
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.meninocoiso.bscm.data.remote.dto.user.SimplifiedUser
import com.meninocoiso.bscm.domain.model.CatalogItem
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.SimplifiedCollection
import com.meninocoiso.bscm.domain.model.Theme
import com.meninocoiso.bscm.domain.model.TourPass
import com.meninocoiso.bscm.domain.model.User
import com.meninocoiso.bscm.domain.model.toSimplifiedUser
import com.meninocoiso.bscm.domain.serialization.ChartParameterType
import com.meninocoiso.bscm.domain.serialization.SimplifiedCollectionParameterType
import com.meninocoiso.bscm.domain.serialization.SimplifiedUserParameterType
import com.meninocoiso.bscm.domain.serialization.TourPassParameterType
import com.meninocoiso.bscm.presentation.screen.collection.Collection
import com.meninocoiso.bscm.presentation.screen.collection.CollectionRoute
import com.meninocoiso.bscm.presentation.screen.collection.CollectionScreen
import com.meninocoiso.bscm.presentation.screen.collection.DeepLinkCollection
import com.meninocoiso.bscm.presentation.screen.details.ChartDetails
import com.meninocoiso.bscm.presentation.screen.details.ChartDetailsRoute
import com.meninocoiso.bscm.presentation.screen.details.ChartDetailsScreen
import com.meninocoiso.bscm.presentation.screen.details.DeepLinkChartDetails
import com.meninocoiso.bscm.presentation.screen.details.DeepLinkTourPassDetails
import com.meninocoiso.bscm.presentation.screen.details.TourPassDetails
import com.meninocoiso.bscm.presentation.screen.details.TourPassDetailsRoute
import com.meninocoiso.bscm.presentation.screen.details.TourPassDetailsScreen
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
fun MainNav(startOAuth: (Uri) -> Unit, user: SimplifiedUser?, hasUpdate: Boolean, intentFlow: Flow<Intent>) {
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

            is TourPass -> {
                // Navigate to tour pass details
                navController.navigate(route = TourPassDetails(tourPass = item)) {
                    launchSingleTop = true
                }
            }

            else -> {
                // For unsupported types, open the web page as a fallback
                startOAuth("https://bscm.netlify.app/link/theme/${item.id}".toUri())
            }
        }
    }

    val onNavigateToCollection = { collection: SimplifiedCollection ->
        navController.navigate(route = Collection(collection = collection)) {
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

    fun navigateToDeepLink(navController: NavController, uri: Uri) {
        val pathSegments = uri.pathSegments

        when (uri.host) {
            "chart" -> {
                val id = pathSegments.getOrNull(0) ?: return
                navController.navigate(DeepLinkChartDetails(id = id)) {
                    launchSingleTop = true
                    // KEY: make sure MainRoute stays at the bottom of the stack
                    restoreState = true
                }
            }
            "profile" -> {
                val username = pathSegments.getOrNull(0) ?: return
                navController.navigate(DeepLinkProfile(username = username)) {
                    launchSingleTop = true
                    restoreState = true
                }
            }
            "collection" -> {
                val username = pathSegments.getOrNull(0) ?: return
                val slug = pathSegments.getOrNull(1) ?: return
                navController.navigate(DeepLinkCollection(username = username, slug = slug)) {
                    launchSingleTop = true
                    restoreState = true
                }
            }
            "tourpass" -> {
                val id = pathSegments.getOrNull(0) ?: return
                navController.navigate(DeepLinkTourPassDetails(id = id)) {
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        intentFlow.collect { intent ->
            // Handle deep links while the app is running
            val uri = intent.data ?: return@collect
            navigateToDeepLink(navController, uri)
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

                // Deep link to chart details
                composableWithTransitions<DeepLinkChartDetails>(
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "bscm://chart/{id}" }
                    )
                ) { backStackEntry ->
                    val chartDetails: DeepLinkChartDetails = backStackEntry.toRoute()
                    ChartDetailsRoute(
                        id = chartDetails.id,
                        onReturn = {
                            navController.navigateUp()
                        },
                        onNavigateToSettings = {
                            navController.popBackStack<MainRoute>(inclusive = false)
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
                        user = if (user?.username == profileRoute.username) user else null,
                        isLoggedIn = user != null,
                        onNavigateToDetails = { chart ->
                            onNavigateToDetails(chart)
                        },
                        onNavigateToCollection = onNavigateToCollection,
                        onReturn = {
                            navController.navigateUp()
                        }
                    )
                }

                // Deep link to collection
                composableWithTransitions<DeepLinkCollection>(
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "bscm://collection/{username}/{slug}" }
                    )
                ) { backStackEntry ->
                    val route: DeepLinkCollection = backStackEntry.toRoute()
                    CollectionRoute(
                        loggedUserId = user?.id,
                        username = route.username,
                        slug = route.slug,
                        onReturn = {
                            navController.navigateUp()
                        },
                        onNavigateToDetails = { chart ->
                            onNavigateToDetails(chart)
                        }
                    )
                }

                // Deep link to tour pass details
                composableWithTransitions<DeepLinkTourPassDetails>(
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "bscm://tourpass/{id}" }
                    )
                ) { backStackEntry ->
                    val tourPassDetails: DeepLinkTourPassDetails = backStackEntry.toRoute()
                    TourPassDetailsRoute(
                        id = tourPassDetails.id,
                        onReturn = {
                            navController.navigateUp()
                        },
                        onNavigateToDetails = { item ->
                            onNavigateToDetails(item)
                        },
                        onNavigateToSettings = {
                            navController.popBackStack<MainRoute>(inclusive = false)
                            onNavigateToSettings()
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
                            navController.popBackStack<MainRoute>(inclusive = false)
                            onNavigateToSettings()
                        }
                    )
                }

                // Tour pass details
                composableWithTransitions<TourPassDetails>(
                    typeMap = mapOf(
                        typeOf<TourPass>() to TourPassParameterType
                    )
                ) { backStackEntry ->
                    val tourPassDetails: TourPassDetails = backStackEntry.toRoute()
                    TourPassDetailsScreen(
                        tourPass = tourPassDetails.tourPass,
                        onReturn = {
                            navController.navigateUp()
                        },
                        onNavigateToChart = { chart ->
                            onNavigateToDetails(chart)
                        },
                        onNavigateToSettings = {
                            navController.popBackStack<MainRoute>(inclusive = false)
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
                        onNavigateToCollection = onNavigateToCollection,
                    )
                }

                // Collection screen
                composableWithTransitions<Collection>(
                    typeMap = mapOf(
                        typeOf<SimplifiedCollection>() to SimplifiedCollectionParameterType
                    )
                ) { backStackEntry ->
                    val route: Collection = backStackEntry.toRoute()
                    CollectionScreen(
                        collection = route.collection,
                        isOwner = user?.id == route.collection.owner.id,
                        onNavigateToDetails = { chart ->
                            onNavigateToDetails(chart)
                        },
                        onReturn = {
                            navController.navigateUp()
                        }
                    )
                }
            }
        }
    }
}