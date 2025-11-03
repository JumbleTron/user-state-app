package com.kielniakodu.userstateapp

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kielniakodu.userstateapp.service.ApiService
import com.kielniakodu.userstateapp.service.LoginRequest
import com.kielniakodu.userstateapp.service.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    object Success : LoginState()
    data class Error(val message: String) : LoginState()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    @param:Named("PublicApiService") private val publicApiService: ApiService,
    private val secureApiService: ApiService,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val response = publicApiService.login(LoginRequest(email, password))
                val responseBody = response.body()
                if (response.isSuccessful && responseBody != null) {
                    sessionManager.saveAuthTokens(
                        authToken = responseBody.token,
                        refreshToken = responseBody.refreshToken
                    )
                    _loginState.value = LoginState.Success
                    Log.d("MainViewModel", "Login successful")
                } else {
                    val errorMessage = when (response.code()) {
                        401 -> "Nieprawidłowe dane logowania"
                        404 -> "Nie znaleziono użytkownika"
                        500 -> "Błąd serwera. Spróbuj ponownie później"
                        else -> "Błąd logowania (${response.code()})"
                    }
                    _loginState.value = LoginState.Error(errorMessage)
                    Log.e("MainViewModel", "Login failed: ${response.code()}")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("Błąd połączenia: ${e.message ?: "Nieznany błąd"}")
                Log.e("MainViewModel", "Login error: ${e.message}")
            }
        }
    }

    fun resetLoginState() {
        _loginState.value = LoginState.Idle
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
