package com.kielniakodu.userstateapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kielniakodu.userstateapp.domain.AuthStatus
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
        observeSessionState()
        observeSessionEvents()
        setContent {
            UserStateAppTheme {
                val viewModel: MainViewModel = hiltViewModel()
                val snackbarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()

                // Observe user data
                val userData = sessionManager.userData

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    Column(
                        verticalArrangement  = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                    ) {
                        Button(onClick = { viewModel.login() }) {
                            Text(text = "Zaloguj")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.clearRefreshToken() }) {
                            Text(text = "Usuń refresh token")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.clearToken() }) {
                            Text(text = "Usuń access token")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.logout() }) {
                            Text(text = "Wyloguj")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = { viewModel.executeApi() }) {
                            Text(text = "API Request")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    private fun observeSessionState() {
        lifecycleScope.launch {
            sessionManager.authStatusFlow.collect { status ->
                withContext(Dispatchers.Main) {
                    when (status) {
                        AuthStatus.AUTHENTICATED -> {
                            val userData = sessionManager.getCurrentUserData()
                            Log.d("MainActivity", "User is authenticated: ${userData?.email}")
                        }
                        AuthStatus.UNAUTHENTICATED -> {
                            navigateToLoginScreen()
                        }
                        AuthStatus.OFFLINE_AUTHENTICATED -> {
                            Log.d("MainActivity", "User is offline authenticated")
                        }
                    }
                }
            }
        }
    }

    private fun observeSessionEvents() {
        lifecycleScope.launch {
            sessionManager.sessionEvents.collect { event ->
                when (event) {
                    SessionEvent.RefreshTokenMissing -> {
                        Log.e("MainActivity", "Refresh token is missing - user will be logged out")
                        // You could show a dialog or snackbar here
                    }
                    SessionEvent.SessionExpired -> {
                        Log.d("MainActivity", "Session expired")
                    }
                }
            }
        }
    }

    private fun navigateToLoginScreen() {
        Log.d("MainActivity", "Navigating to LoginActivity")
//        runOnUiThread {
//            val intent = Intent(this, LoginActivity::class.java)
//            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//            startActivity(intent)
//            finish()
//        }
    }
}
