package com.kielniakodu.userstateapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kielniakodu.userstateapp.domain.AuthStatus
import com.kielniakodu.userstateapp.service.SessionManager
import com.kielniakodu.userstateapp.ui.screens.HomeScreen
import com.kielniakodu.userstateapp.ui.screens.LoginScreen
import com.kielniakodu.userstateapp.ui.screens.ProfileScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    sessionManager: SessionManager,
    startDestination: String = Screen.Login.route
) {
    val authStatus by sessionManager.authStatusFlow.collectAsStateWithLifecycle()

    // Observe auth status and navigate accordingly
    LaunchedEffect(authStatus) {
        when (authStatus) {
            AuthStatus.AUTHENTICATED, AuthStatus.OFFLINE_AUTHENTICATED -> {
                // User is authenticated, ensure we're not on login screen
                if (navController.currentBackStackEntry?.destination?.route == Screen.Login.route) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            }
            AuthStatus.UNAUTHENTICATED -> {
                // User is not authenticated, navigate to login
                if (navController.currentBackStackEntry?.destination?.route != Screen.Login.route) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                sessionManager = sessionManager
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                sessionManager = sessionManager
            )
        }
    }
}