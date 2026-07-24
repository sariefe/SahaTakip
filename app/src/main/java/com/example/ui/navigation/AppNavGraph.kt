package com.example.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BiometricLockScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EventLogsScreen
import com.example.ui.screens.LeaveRequestScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.runtime.CompositionLocalProvider
import com.example.util.LocalLanguage
import com.example.ui.screens.MapTrackingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.viewmodel.MainViewModel

@Composable
fun AppNavGraph(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val userProfile by viewModel.userProfile.collectAsState()
    val isAuthenticated by viewModel.isAuthenticated.collectAsState()
    val currentLang by viewModel.language.collectAsState()

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

    CompositionLocalProvider(LocalLanguage provides currentLang) {
        Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    bottomNavScreens.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        // Use startDestination as the anchor for popping
                                        popUpTo(startDestination) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        
                                        // Disable restoreState for Dashboard to prevent freezes
                                        if (screen.route != Screen.Dashboard.route) {
                                            restoreState = true
                                        }
                                    }
                                }
                            },
                            icon = {
                                screen.icon?.let {
                                    Icon(imageVector = it, contentDescription = screen.titleTr)
                                }
                            },
                            label = {
                                val labelText = if (screen.stringResId != null) {
                                    androidx.compose.ui.res.stringResource(id = screen.stringResId)
                                } else {
                                    if (currentLang == "tr") screen.titleTr else screen.titleEn
                                }
                                Text(
                                    text = labelText,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
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
                    viewModel = viewModel,
                    onAuthSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.BiometricLock.route) {
                BiometricLockScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.BiometricLock.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToMap = { navController.navigate(Screen.TrackingMap.route) },
                    onNavigateToLogs = { navController.navigate(Screen.EventLogs.route) }
                )
            }

            composable(Screen.TrackingMap.route) {
                MapTrackingScreen(viewModel = viewModel)
            }

            composable(Screen.EventLogs.route) {
                EventLogsScreen(viewModel = viewModel)
            }

            composable(Screen.LeaveRequests.route) {
                LeaveRequestScreen(viewModel = viewModel)
            }

            composable(Screen.Settings.route) {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
}
