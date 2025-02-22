package com.meninocoiso.beatstarcommunity.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.meninocoiso.beatstarcommunity.domain.model.Chart
import com.meninocoiso.beatstarcommunity.domain.serialization.ChartParameterType
import com.meninocoiso.beatstarcommunity.presentation.screens.ChartDetails
import com.meninocoiso.beatstarcommunity.presentation.screens.ChartDetailsScreen
import kotlin.reflect.typeOf

@Composable
fun MainNav() {
    val navController = rememberNavController()
    val bottomNavController = rememberNavController()

    NavHost(navController = navController, startDestination = MainRoute) {
        composableWithTransitions<ChartDetails>(
            typeMap = mapOf(
                typeOf<Chart>() to ChartParameterType
            )
        ) { backStackEntry ->
            val chartDetails: ChartDetails = backStackEntry.toRoute()
            ChartDetailsScreen(
                onReturn = {
                    navController.navigateUp()
                },
                chart = chartDetails.chart
            )
        }

        composableWithoutTransitions<MainRoute> {
            BottomNav(bottomNavController, navController)
        }
    }
}