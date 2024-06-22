package com.meninocoiso.beatstarcommunity.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.meninocoiso.beatstarcommunity.screens.ChartDetailsScreen
import com.meninocoiso.beatstarcommunity.screens.Settings
import com.meninocoiso.beatstarcommunity.screens.Updates
import com.meninocoiso.beatstarcommunity.screens.Workspace
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