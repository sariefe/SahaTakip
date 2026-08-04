package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BiometricLockScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EventLogsScreen
import com.example.ui.screens.LeaveRequestScreen
import com.example.ui.screens.MapTrackingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.DeviceViewModel
import com.example.ui.viewmodel.RequestLogViewModel
import com.example.ui.viewmodel.SettingsViewModel
import com.example.ui.viewmodel.TrackingViewModel
import com.example.util.LocalLanguage
import com.example.util.tr

@Composable
fun AppNavGraph(
    windowSizeClass: WindowSizeClass
) {
    val deviceViewModel: DeviceViewModel = hiltViewModel()
    val trackingViewModel: TrackingViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val requestLogViewModel: RequestLogViewModel = hiltViewModel()
    val authViewModel: AuthViewModel = hiltViewModel()

    val navController = rememberNavController()
    val userProfile by trackingViewModel.userProfile.collectAsState()
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
    val currentLang by settingsViewModel.language.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    if (userProfile == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val isActivated = userProfile?.isActivated == true
    val startDestination = when {
        !isActivated -> Screen.Auth.route
        !isAuthenticated -> Screen.BiometricLock.route
        else -> Screen.Dashboard.route
    }

    val showBottomBar = currentRoute in bottomNavScreens.map { it.route }
    val useNavRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact && showBottomBar

    CompositionLocalProvider(LocalLanguage provides currentLang) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (useNavRail) {
                AppNavRail(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                popUpTo(startDestination) { saveState = true }
                                launchSingleTop = true
                                if (route != Screen.Dashboard.route) restoreState = true
                            }
                        }
                    }
                )
            }
            
            Scaffold(
                modifier = Modifier.weight(1f),
                bottomBar = {
                    if (showBottomBar && !useNavRail) {
                        AppBottomBar(
                            currentRoute = currentRoute,
                            startDestination = startDestination,
                            navController = navController
                        )
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(Screen.Auth.route) {
                        AuthScreen(
                            viewModel = authViewModel,
                            onAuthSuccess = {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.Auth.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.BiometricLock.route) {
                        BiometricLockScreen(
                            viewModel = trackingViewModel,
                            authViewModel = authViewModel,
                            onLoginSuccess = {
                                navController.navigate(Screen.Dashboard.route) {
                                    popUpTo(Screen.BiometricLock.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Screen.Dashboard.route) {
                        DashboardScreen(
                            deviceViewModel = deviceViewModel,
                            trackingViewModel = trackingViewModel,
                            authViewModel = authViewModel,
                            onNavigateToMap = { navController.navigate(Screen.TrackingMap.route) },
                            windowWidthSizeClass = windowSizeClass.widthSizeClass
                        )
                    }

                    composable(Screen.TrackingMap.route) {
                        MapTrackingScreen(
                            viewModel = trackingViewModel,
                            windowWidthSizeClass = windowSizeClass.widthSizeClass
                        )
                    }

                    composable(Screen.EventLogs.route) {
                        EventLogsScreen(
                            viewModel = requestLogViewModel,
                            windowWidthSizeClass = windowSizeClass.widthSizeClass
                        )
                    }

                    composable(Screen.LeaveRequests.route) {
                        LeaveRequestScreen(
                            viewModel = requestLogViewModel,
                            windowWidthSizeClass = windowSizeClass.widthSizeClass
                        )
                    }

                    composable(Screen.Settings.route) {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            windowWidthSizeClass = windowSizeClass.widthSizeClass
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppBottomBar(
    currentRoute: String?,
    startDestination: String,
    navController: NavController
) {
    NavigationBar(
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        bottomNavScreens.forEach { screen ->
            val selected = currentRoute == screen.route
            NavigationBarItem(
                selected = selected,
                alwaysShowLabel = true,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(startDestination) {
                                saveState = true
                            }
                            launchSingleTop = true
                            if (screen.route != Screen.Dashboard.route) {
                                restoreState = true
                            }
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ),
                icon = {
                    screen.icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = screen.titleTr,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = tr(screen.titleTr, screen.titleEn),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Visible
                    )
                }
            )
        }
    }
}

@Composable
fun AppNavRail(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight()
    ) {
        bottomNavScreens.forEach { screen ->
            val selected = currentRoute == screen.route
            NavigationRailItem(
                selected = selected,
                onClick = { onNavigate(screen.route) },
                icon = {
                    screen.icon?.let {
                        Icon(
                            imageVector = it,
                            contentDescription = screen.titleTr,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                label = {
                    Text(
                        text = tr(screen.titleTr, screen.titleEn),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                )
            )
        }
}
}
