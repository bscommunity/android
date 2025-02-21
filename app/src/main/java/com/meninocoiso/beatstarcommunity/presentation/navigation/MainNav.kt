package com.meninocoiso.beatstarcommunity.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.meninocoiso.beatstarcommunity.presentation.screens.ChartDetails
import com.meninocoiso.beatstarcommunity.presentation.screens.ChartDetailsScreen

@Composable
fun MainNav() {
    val navController = rememberNavController()
    val bottomNavController = rememberNavController()

    NavHost(navController = navController, startDestination = MainRoute) {
        composableWithTransitions<ChartDetails> { backStackEntry ->
            val chartDetails: ChartDetails = backStackEntry.toRoute()
            ChartDetailsScreen(onReturn = {
                navController.navigateUp()
            })
        }

        composableWithoutTransitions<MainRoute> {
            BottomNav(bottomNavController, navController)
        }
    }
}