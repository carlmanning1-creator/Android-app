package com.rawbarbell.club.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.rawbarbell.club.ui.screens.home.HomeScreen
import com.rawbarbell.club.ui.screens.programs.ProgramsScreen
import com.rawbarbell.club.ui.screens.maxes.MaxesScreen
import com.rawbarbell.club.ui.screens.journal.JournalScreen
import com.rawbarbell.club.ui.screens.programdetail.ProgramDetailScreen
import com.rawbarbell.club.ui.screens.session.SessionScreen
import com.rawbarbell.club.ui.screens.builder.ProgramBuilderScreen
import com.rawbarbell.club.ui.screens.importsheet.ImportSheetScreen

private data class BottomNavItem(
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val screen: Screen
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val bottomNavItems = listOf(
        BottomNavItem("Home", Icons.Filled.Home, Screen.Home),
        BottomNavItem("Programs", Icons.Filled.ListAlt, Screen.Programs),
        BottomNavItem("Maxes", Icons.Filled.FitnessCenter, Screen.Maxes),
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in listOf(
        Screen.Home.route,
        Screen.Programs.route,
        Screen.Maxes.route,
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentDestination?.hierarchy?.any {
                                it.route == item.screen.route
                            } == true,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(navController = navController)
            }
            composable(Screen.Programs.route) {
                ProgramsScreen(navController = navController)
            }
            composable(Screen.Maxes.route) {
                MaxesScreen()
            }
            composable(
                route = Screen.Journal.route,
                arguments = listOf(navArgument("weekId") { type = NavType.StringType })
            ) {
                JournalScreen(navController = navController)
            }
            composable(
                route = Screen.ProgramDetail.route,
                arguments = listOf(navArgument("programId") { type = NavType.StringType })
            ) {
                ProgramDetailScreen(navController = navController)
            }
            composable(
                route = Screen.Session.route,
                arguments = listOf(
                    navArgument("dayId") { type = NavType.StringType },
                    navArgument("weekId") { type = NavType.StringType }
                )
            ) {
                SessionScreen(navController = navController)
            }
            composable(Screen.ProgramBuilder.route) {
                ProgramBuilderScreen(navController = navController)
            }
            composable(Screen.ImportSheet.route) {
                ImportSheetScreen(navController = navController)
            }
        }
    }
}
