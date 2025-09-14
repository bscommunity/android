package com.meninocoiso.bscm.presentation.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.model.User
import com.meninocoiso.bscm.domain.serialization.ChartParameterType
import com.meninocoiso.bscm.presentation.screens.details.ChartDetails
import com.meninocoiso.bscm.presentation.screens.details.ChartDetailsRoute
import com.meninocoiso.bscm.presentation.screens.details.ChartDetailsScreen
import com.meninocoiso.bscm.presentation.screens.details.DeepLinkChartDetails
import com.meninocoiso.bscm.presentation.screens.settings.Profile
import com.meninocoiso.bscm.presentation.screens.settings.ProfileScreen
import kotlinx.serialization.Serializable
import kotlin.reflect.typeOf

@Serializable
object MainRoute

@Composable
fun MainNav(startOAuth: (Uri) -> Unit, hasUpdate: Boolean, cacheUser: User?) {
    val navController = rememberNavController()
    val bottomNavController = rememberNavController()

    NavHost(navController = navController, startDestination = MainRoute) {
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

        // Chart details
        composableWithTransitions<Profile> { backStackEntry ->
            val profile: Profile = backStackEntry.toRoute()
            ProfileScreen(
                id = profile.id,
                onReturn = {
                    navController.navigateUp()
                }
            )
        }

        composableWithoutTransitions<MainRoute> {
            BottomNav(bottomNavController, navController, hasUpdate, startOAuth, cacheUser)
        }
    }
}