package com.meninocoiso.beatstarcommunity.presentation.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.meninocoiso.beatstarcommunity.presentation.ui.components.layout.LaunchAppButton
import com.meninocoiso.beatstarcommunity.presentation.screens.ChartDetailsScreen
import com.meninocoiso.beatstarcommunity.presentation.screens.SettingsScreen
import com.meninocoiso.beatstarcommunity.presentation.screens.UpdatesScreen
import com.meninocoiso.beatstarcommunity.presentation.screens.WorkspaceScreen
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