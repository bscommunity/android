package com.meninocoiso.bscm.presentation.navigation

import android.annotation.SuppressLint
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import kotlin.reflect.KClass

data class BottomNavigationItem(
    val route: Route,
    val title: String,
    val selectedIcon: Int,
    val unselectedIcon: Int,
    val hasNews: Boolean,
    val badgeCount: Int? = null
)

private fun NavDestination?.isRouteInHierarchy(route: KClass<*>) =
    this?.hierarchy?.any {
        it.hasRoute(route)
    } == true

@Composable
fun BottomNavBar(
    onClick: (Route) -> Unit,
    navBackStackEntry: NavBackStackEntry?,
    bottomNavigationItems: List<BottomNavigationItem>
) {
    NavigationBar {
        bottomNavigationItems.forEach { item ->
            val currentDestination = navBackStackEntry?.destination
            val isSelected = currentDestination.isRouteInHierarchy(item.route::class)

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    // Prevent navigating to the same destination
                    if (isSelected) return@NavigationBarItem
                    
                    onClick(item.route)
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
                            painter = if (isSelected) {
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