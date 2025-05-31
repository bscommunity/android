package com.meninocoiso.bscm.presentation.navigation

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import com.meninocoiso.bscm.domain.enums.UpdatesSection
import kotlinx.serialization.Serializable

@Serializable
sealed class Route {
	@Serializable
	object Workshop : Route()
	@Serializable
	data class Updates(val section: UpdatesSection = UpdatesSection.Workshop) : Route()
	@Serializable
	object Settings : Route()
}

data class BottomNavigationItem(
	val route: Route,
	val title: String,
	val selectedIcon: Int,
	val unselectedIcon: Int,
	val hasNews: Boolean,
	val badgeCount: Int? = null
)

@Composable
fun BottomNavBar(
	selectedItemIndex: Int,
	setItemIndex: (Int) -> Unit,
	navController: NavHostController,
	bottomNavigationItems: List<BottomNavigationItem>
) {
	NavigationBar {
		bottomNavigationItems.forEachIndexed { index, item ->
			NavigationBarItem(
				selected = selectedItemIndex == index,
				onClick = {
					// Prevent navigating to the same destination
					if (selectedItemIndex == index) return@NavigationBarItem

					setItemIndex(index)

					navController.navigate(route = item.route) {
						// Pop up to the start destination of the graph to
						// avoid building up a large stack of destinations
						// on the back stack as users select items
						popUpTo(navController.graph.startDestinationId) {
							saveState = true
						}

						// Avoid multiple copies of the same destination when
						// reselecting the same item
						launchSingleTop = true

						// Restore cacheState when reselecting a previously selected item
						restoreState = true
					}
				},
				label = {
					Text(text = item.title, textAlign = TextAlign.Center)
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