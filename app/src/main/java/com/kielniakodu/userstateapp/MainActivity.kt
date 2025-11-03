package com.kielniakodu.userstateapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.kielniakodu.userstateapp.domain.AuthStatus
import com.kielniakodu.userstateapp.navigation.NavGraph
import com.kielniakodu.userstateapp.navigation.Screen
import com.kielniakodu.userstateapp.service.SessionEvent
import com.kielniakodu.userstateapp.service.SessionManager
import com.kielniakodu.userstateapp.ui.theme.UserStateAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager.loadInitialAuthStatus()
        observeSessionEvents()

        setContent {
            UserStateAppTheme {
                val authStatus by sessionManager.authStatusFlow.collectAsStateWithLifecycle()
                val navController = rememberNavController()
                val snackbarHostState = remember { SnackbarHostState() }

                // Determine start destination based on auth status
                val startDestination = when (authStatus) {
                    AuthStatus.AUTHENTICATED, AuthStatus.OFFLINE_AUTHENTICATED -> Screen.Home.route
                    AuthStatus.UNAUTHENTICATED -> Screen.Login.route
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { paddingValues ->
                    Surface(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NavGraph(
                            navController = navController,
                            sessionManager = sessionManager,
                            startDestination = startDestination
                        )
                    }
                }

                // Handle back button - prevent going back to login after authenticated
                BackHandler(enabled = authStatus != AuthStatus.UNAUTHENTICATED) {
                    if (navController.previousBackStackEntry != null) {
                        navController.popBackStack()
                    }
                }
            }
        }
    }

    private fun observeSessionEvents() {
        lifecycleScope.launch {
            sessionManager.sessionEvents.collect { event ->
                withContext(Dispatchers.Main) {
                    when (event) {
                        SessionEvent.RefreshTokenMissing -> {
                            Log.e("MainActivity", "Refresh token is missing - user will be logged out")
                        }
                        SessionEvent.SessionExpired -> {
                            Log.d("MainActivity", "Session expired")
                        }
                    }
                }
            }
        }
    }
}