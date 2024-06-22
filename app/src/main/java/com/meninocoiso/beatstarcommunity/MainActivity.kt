package com.meninocoiso.beatstarcommunity

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.meninocoiso.beatstarcommunity.navigation.BottomNavigationComponent
import com.meninocoiso.beatstarcommunity.navigation.SettingsScreen
import com.meninocoiso.beatstarcommunity.navigation.UpdatesScreen
import com.meninocoiso.beatstarcommunity.navigation.WorkspaceScreen
import com.meninocoiso.beatstarcommunity.screens.ChartDetails
import com.meninocoiso.beatstarcommunity.screens.ChartDetailsScreen
import com.meninocoiso.beatstarcommunity.screens.Settings
import com.meninocoiso.beatstarcommunity.screens.Updates
import com.meninocoiso.beatstarcommunity.screens.Workspace
import com.meninocoiso.beatstarcommunity.ui.theme.BeatstarCommunityTheme
import kotlinx.serialization.Serializable

@Serializable
object MainRoute

class MainActivity : ComponentActivity() {
	@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
	@OptIn(ExperimentalMaterial3Api::class)
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		// This app draws behind the system bars, so we want to handle fitting system windows
		enableEdgeToEdge()
		setContent {
			BeatstarCommunityTheme {
				val navController = rememberNavController()
				val bottomNavController = rememberNavController()

				NavHost(navController = navController, startDestination = MainRoute) {
					composable<ChartDetailsScreen> {
						ChartDetails()
					}

					composable<MainRoute> {
						Scaffold(
							bottomBar = {
								BottomNavigationComponent(navController = bottomNavController)
							}
						) { innerPadding ->
							NavHost(
								modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
								navController = bottomNavController,
								startDestination = WorkspaceScreen,
							) {
								composable<WorkspaceScreen> {
									Workspace(onNavigateToDetails = {
										navController.navigate(ChartDetailsScreen)
									})
								}
								composable<UpdatesScreen> {
									Updates()
								}
								composable<SettingsScreen> {
									Settings()
								}
							}
						}
					}
				}
			}
		}
	}
}