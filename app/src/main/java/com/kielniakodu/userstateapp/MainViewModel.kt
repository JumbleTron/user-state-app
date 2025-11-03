package com.kielniakodu.userstateapp

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kielniakodu.userstateapp.service.ApiService
import com.kielniakodu.userstateapp.service.LoginRequest
import com.kielniakodu.userstateapp.service.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class MainViewModel @Inject constructor(
    @param:Named("PublicApiService") private val publicApiService: ApiService,
    private val secureApiService: ApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    // NOTE: For demonstration purposes only
    // In a real app, credentials should come from user input
    fun login(email: String = "demo@example.com", password: String = "demo123") {
        viewModelScope.launch {
            try {
                val response = publicApiService.login(LoginRequest(email, password))
                val responseBody = response.body()
                if (response.isSuccessful && responseBody != null) {
                    sessionManager.saveAuthTokens(
                        authToken = responseBody.token,
                        refreshToken = responseBody.refreshToken
                    )
                    Log.d("MainViewModel", "Login successful")
                } else {
                    Log.e("MainViewModel", "Login failed: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Login error: ${e.message}")
            }
        }
    }

    fun clearToken() {
        sessionManager.clearToken()
    }

    fun clearRefreshToken() {
        sessionManager.clearRefreshToken()
    }

    fun logout() {
        sessionManager.clearSession()
    }

    fun executeApi() {
        viewModelScope.launch {
            try {
                val response = secureApiService.getMessages()
                if (response.isSuccessful) {
                    Log.d("MainViewModel", "Response: ${response.body()}")
                } else {
                    Log.d("MainViewModel", "Error: ${response.code()}")
                }
            } catch(e: Exception) {
                Log.d("MainViewModel", "Error: ${e.message}")
            }
        }
    }
}
