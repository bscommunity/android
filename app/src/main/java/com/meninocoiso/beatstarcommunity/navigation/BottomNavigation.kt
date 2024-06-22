package com.meninocoiso.beatstarcommunity.navigation

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import com.meninocoiso.beatstarcommunity.R
import kotlinx.serialization.Serializable

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
					navController.navigate(item.route) {
						// Pop up to the start destination of the graph to
						// avoid building up a large stack of destinations
						// on the back stack as users select items
						popUpTo(navController.graph.startDestinationId) {
							saveState = true
						}

						// Avoid multiple copies of the same destination when
						// reselecting the same item
						launchSingleTop = true

						// Restore state when reselecting a previously selected item
						restoreState = true
					}
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