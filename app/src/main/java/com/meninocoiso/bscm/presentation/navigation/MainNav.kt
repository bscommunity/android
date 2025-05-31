package com.meninocoiso.bscm.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import com.meninocoiso.bscm.domain.model.Chart
import com.meninocoiso.bscm.domain.serialization.ChartParameterType
import com.meninocoiso.bscm.presentation.screens.details.ChartDetails
import com.meninocoiso.bscm.presentation.screens.details.ChartDetailsRoute
import com.meninocoiso.bscm.presentation.screens.details.ChartDetailsScreen
import com.meninocoiso.bscm.presentation.screens.details.DeepLinkChartDetails
import kotlin.reflect.typeOf

@Composable
fun MainNav(hasUpdate: Boolean) {
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

        composableWithoutTransitions<MainRoute> {
            BottomNav(bottomNavController, navController, hasUpdate)
        }
    }
}