package com.meninocoiso.beatstarcommunity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.meninocoiso.beatstarcommunity.navigation.MainNav
import com.meninocoiso.beatstarcommunity.navigation.MainRoute
import com.meninocoiso.beatstarcommunity.navigation.composableWithTransitions
import com.meninocoiso.beatstarcommunity.screens.ChartDetails
import com.meninocoiso.beatstarcommunity.screens.ChartDetailsScreen
import com.meninocoiso.beatstarcommunity.ui.theme.BeatstarCommunityTheme

class MainActivity : ComponentActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		// This app draws content behind the system bars (status and navigation bar),
		// so we want to handle fitting system windows
		enableEdgeToEdge()
		setContent {
			BeatstarCommunityTheme {
				val navController = rememberNavController()
				val bottomNavController = rememberNavController()

				NavHost(navController = navController, startDestination = MainRoute) {
					composableWithTransitions<ChartDetailsScreen> {
						ChartDetails(onReturn = {
							navController.navigateUp()
						})
					}

					composableWithTransitions<MainRoute> {
						MainNav(bottomNavController, navController)
					}
				}
			}
		}
	}
}