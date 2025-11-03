package com.kielniakodu.userstateapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.kielniakodu.userstateapp.MainViewModel
import com.kielniakodu.userstateapp.service.SessionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToProfile: () -> Unit,
    onLogout: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
    sessionManager: SessionManager
) {
    val userData by sessionManager.userData.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // User info card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Informacje o użytkowniku",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    userData?.let { user ->
                        Text("Email: ${user.email}")
                        Text("User ID: ${user.userId}")
                        if (user.username != null) {
                            Text("Username: ${user.username}")
                        }
                        if (user.roles.isNotEmpty()) {
                            Text("Roles: ${user.roles.joinToString(", ")}")
                        }
                    } ?: Text("Brak danych użytkownika")
                }
            }

            // Action buttons
            Button(
                onClick = { viewModel.executeApi() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text("Wykonaj API Request")
            }

            Button(
                onClick = onNavigateToProfile,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Text("Przejdź do Profilu")
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp))

            // Debug buttons
            OutlinedButton(
                onClick = { viewModel.clearToken() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text("Usuń Access Token (test)")
            }

            OutlinedButton(
                onClick = { viewModel.clearRefreshToken() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text("Usuń Refresh Token (test)")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.logout()
                    onLogout()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Wyloguj")
            }
        }
    }
}