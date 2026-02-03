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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.User
import com.meninocoiso.bscm.domain.serialization.ChartParameterType
import com.meninocoiso.bscm.domain.serialization.UserParameterType
import com.meninocoiso.bscm.presentation.screen.details.ChartDetails
import com.meninocoiso.bscm.presentation.screen.details.ChartDetailsRoute
import com.meninocoiso.bscm.presentation.screen.details.ChartDetailsScreen
import com.meninocoiso.bscm.presentation.screen.details.DeepLinkChartDetails
import com.meninocoiso.bscm.presentation.screen.settings.Collection
import com.meninocoiso.bscm.presentation.screen.settings.CollectionScreen
import com.meninocoiso.bscm.presentation.screen.settings.Profile
import com.meninocoiso.bscm.presentation.screen.settings.ProfileScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

@Serializable
object MainRoute

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainNav(startOAuth: (Uri) -> Unit, hasUpdate: Boolean, user: User?, intentFlow: Flow<Intent>, ) {
    val navController = rememberNavController()
    val bottomNavController = rememberNavController()

    val onNavigateToDetails = { chart: Chart ->
        navController.navigate(route = ChartDetails(chart = chart)) {
            // Prevent users from opening multiple details screens
            launchSingleTop = true
        }
    }

    val onNavigateToCollection = { collectionId: String ->
        navController.navigate(route = Collection(collectionId = collectionId)) {
            // Prevent users from opening multiple collection screens
            launchSingleTop = true
        }
    }

    LaunchedEffect(Unit) {
        intentFlow.collect { intent ->
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
                        navDeepLink { uriPattern = "bscm://chart/{chartId}" }
                    )
                ) { backStackEntry ->
                    val chartDetails: DeepLinkChartDetails = backStackEntry.toRoute()
                    ChartDetailsRoute(
                        chartId = chartDetails.chartId,
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
                        }
                    )
                }

                // Profile screen
                composable<Profile>(
                    typeMap = mapOf(
                        typeOf<User>() to UserParameterType
                    )
                ) { backStackEntry ->
                    val profile: Profile = backStackEntry.toRoute()
                    ProfileScreen(
                        this@SharedTransitionLayout,
                        this,
                        user = profile.user,
                        userId = profile.user.id,
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
                        hasUpdate,
                        startOAuth,
                        user
                    )
                }
            }
        }
    }
}