package com.meninocoiso.beatstarcommunity.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.meninocoiso.beatstarcommunity.components.LaunchAppButton
import com.meninocoiso.beatstarcommunity.screens.chartdetails.ChartDetailsScreen
import com.meninocoiso.beatstarcommunity.screens.settings.SettingsScreen
import com.meninocoiso.beatstarcommunity.screens.updates.UpdatesScreen
import com.meninocoiso.beatstarcommunity.screens.workspace.WorkspaceScreen
import kotlinx.serialization.Serializable

@Serializable
object MainRoute

@Composable
fun MainNav(
	bottomNavController: NavHostController,
	navController: NavHostController
) {
	Scaffold(
		bottomBar = {
			BottomNavigationComponent(navController = bottomNavController)
		},
		floatingActionButton = {
			LaunchAppButton()
		}
	) { innerPadding ->
		NavHost(
			modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
			navController = bottomNavController,
			startDestination = WorkspaceScreen,
		) {
			composableWithFade<WorkspaceScreen> {
				WorkspaceScreen(onNavigateToDetails = {
					navController.navigate(ChartDetailsScreen)
				})
			}
			composableWithFade<UpdatesScreen> {
				UpdatesScreen()
			}
			composableWithFade<SettingsScreen> {
				SettingsScreen()
			}
		}
	}
}