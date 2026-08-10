package ph.attendance.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ph.attendance.ui.dashboard.DashboardScreen
import ph.attendance.ui.reports.ReportsScreen
import ph.attendance.ui.scanner.ScannerScreen
import ph.attendance.ui.schedules.SchedulesScreen
import ph.attendance.ui.sections.SectionsScreen
import ph.attendance.ui.sections.StudentsScreen
import ph.attendance.ui.sync.SyncScreen
import ph.attendance.ui.theme.AttendanceTheme

private data class Destination(val route: String, val label: String, val icon: ImageVector)

private val DESTINATIONS = listOf(
    Destination("today", "Today", Icons.Filled.Today),
    Destination("scan", "Scan", Icons.Filled.QrCodeScanner),
    Destination("sections", "Sections", Icons.Filled.Groups),
    Destination("schedules", "Schedules", Icons.Filled.CalendarMonth),
    Destination("reports", "Reports", Icons.Filled.Insights),
    Destination("sync", "Sync", Icons.Filled.Sync),
)

@Composable
fun AttendanceApp() {
    AttendanceTheme {
        val viewModel = appViewModel()
        val navController = rememberNavController()
        val snackbars = remember { SnackbarHostState() }

        val backStack by navController.currentBackStackEntryAsState()
        val currentRoute = backStack?.destination

        val message by viewModel.message.collectAsStateWithLifecycle()
        LaunchedEffect(message) {
            message?.let {
                snackbars.showSnackbar(it)
                viewModel.messageShown()
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbars) },
            bottomBar = {
                NavigationBar {
                    DESTINATIONS.forEach { destination ->
                        val selected = currentRoute?.hierarchy?.any { it.route == destination.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(destination.route) {
                                    // Tapping a tab returns to its root rather than stacking a
                                    // second copy, and the back button still leaves the app from
                                    // the start destination.
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(destination.icon, contentDescription = null) },
                            label = { Text(destination.label) },
                        )
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "today",
                modifier = Modifier.padding(padding),
                // Horizontal slide for lateral moves, so a tab change reads as a change of place
                // rather than a redraw. 220 ms: long enough to follow, short enough not to wait.
                enterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(220)) +
                        fadeIn(tween(220))
                },
                exitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(180)) +
                        fadeOut(tween(180))
                },
                popEnterTransition = {
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(220)) +
                        fadeIn(tween(220))
                },
                popExitTransition = {
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(180)) +
                        fadeOut(tween(180))
                },
            ) {
                composable("today") { DashboardScreen(viewModel) }
                composable("scan") { ScannerScreen(viewModel) }
                composable("sections") {
                    SectionsScreen(viewModel, onOpenSection = { navController.navigate("students/$it") })
                }
                composable("students/{sectionId}") { entry ->
                    StudentsScreen(
                        viewModel = viewModel,
                        sectionId = entry.arguments?.getString("sectionId").orEmpty(),
                        onBack = { navController.popBackStack() },
                    )
                }
                composable("schedules") { SchedulesScreen(viewModel) }
                composable("reports") { ReportsScreen(viewModel) }
                composable("sync") { SyncScreen(viewModel) }
            }
        }
    }
}
