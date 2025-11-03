package com.kielniakodu.userstateapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.kielniakodu.userstateapp.LoginState
import com.kielniakodu.userstateapp.MainViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("demo@example.com") }
    var password by remember { mutableStateOf("demo123") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val loginState by viewModel.loginState.collectAsState()

    // Handle login state changes
    LaunchedEffect(loginState) {
        when (loginState) {
            is LoginState.Success -> {
                viewModel.resetLoginState()
                onLoginSuccess()
            }
            is LoginState.Error -> {
                errorMessage = (loginState as LoginState.Error).message
            }
            else -> {
                // Idle or Loading - do nothing
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Zaloguj się",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Error message display
        errorMessage?.let { error ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = null // Clear error when user types
            },
            label = { Text("Email") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            enabled = loginState !is LoginState.Loading,
            isError = errorMessage != null
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null // Clear error when user types
            },
            label = { Text("Hasło") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            enabled = loginState !is LoginState.Loading,
            isError = errorMessage != null
        )

        Button(
            onClick = {
                errorMessage = null
                viewModel.login(email, password)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            enabled = loginState !is LoginState.Loading && email.isNotBlank() && password.isNotBlank()
        ) {
            if (loginState is LoginState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text("Zaloguj")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Demo credentials:\ndemo@example.com / demo123",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}