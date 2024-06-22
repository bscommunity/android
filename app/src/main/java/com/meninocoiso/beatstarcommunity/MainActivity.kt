package com.meninocoiso.beatstarcommunity

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.meninocoiso.beatstarcommunity.screens.Settings
import com.meninocoiso.beatstarcommunity.screens.Updates
import com.meninocoiso.beatstarcommunity.screens.Workspace
import com.meninocoiso.beatstarcommunity.ui.theme.BeatstarCommunityTheme
import kotlinx.serialization.Serializable

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

				Scaffold(
					bottomBar = {
						BottomNavigationComponent(navController = navController)
					}
				) { innerPadding ->
					NavHost(
						modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
						navController = navController,
						startDestination = WorkspaceScreen,
					) {
						composable<WorkspaceScreen> {
							Workspace()
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

@Serializable
object WorkspaceScreen

@Serializable
object UpdatesScreen

@Serializable
object SettingsScreen

data class BottomNavigationItem(
	val route: Any,
	val title: String,
	val selectedIcon: Int,
	val unselectedIcon: Int,
	val hasNews: Boolean,
	val badgeCount: Int? = null
)

val bottomNavigationItems = listOf(
	BottomNavigationItem(
		route = WorkspaceScreen,
		title = "Workspace",
		selectedIcon = R.drawable.baseline_library_music_24,
		unselectedIcon = R.drawable.outline_library_music_24,
		hasNews = false
	),
	BottomNavigationItem(
		route = UpdatesScreen,
		title = "Updates",
		selectedIcon = R.drawable.baseline_deployed_code_24,
		unselectedIcon = R.drawable.outline_deployed_code_24,
		hasNews = false
	),
	BottomNavigationItem(
		route = SettingsScreen,
		title = "Settings",
		selectedIcon = R.drawable.baseline_settings_24,
		unselectedIcon = R.drawable.outline_settings_24,
		hasNews = false
	)
)

@Composable
fun BottomNavigationComponent(navController: NavHostController) {
	NavigationBar {
		var selectedItemIndex by rememberSaveable {
			mutableIntStateOf(0)
		}

		bottomNavigationItems.forEachIndexed { index, item ->
			NavigationBarItem(
				selected = selectedItemIndex == index,
				onClick = {
					selectedItemIndex = index
					navController.navigate(item.route)
				},
				label = {
					Text(text = item.title)
				},
				alwaysShowLabel = true,
				icon = {
					BadgedBox(
						badge = {
							if (item.badgeCount != null) {
								Badge {
									Text(text = item.badgeCount.toString())
								}
							} else if (item.hasNews) {
								Badge()
							}
						}
					) {
						Icon(
							painter = if (index == selectedItemIndex) {
								painterResource(id = item.selectedIcon)
							} else painterResource(id = item.unselectedIcon),
							contentDescription = item.title
						)
					}
				}
			)
		}
	}
}
