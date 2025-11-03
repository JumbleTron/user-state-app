package com.kielniakodu.userstateapp.service

import androidx.datastore.core.DataStore
import com.kielniakodu.userstateapp.domain.AuthStatus
import com.kielniakodu.userstateapp.domain.UserData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

sealed class SessionEvent {
    object RefreshTokenMissing : SessionEvent()
    object SessionExpired : SessionEvent()
}

@Singleton
class SessionManager @Inject constructor(
    private val secureTokenDataStore: DataStore<AuthTokens>,
    private val jwtManager: JwtManager
) {

    private val _authStatus = MutableStateFlow<AuthStatus>(getInitialAuthStatus())
    val authStatusFlow: StateFlow<AuthStatus> = _authStatus.asStateFlow()

    private val _sessionEvents = MutableSharedFlow<SessionEvent>()
    val sessionEvents: SharedFlow<SessionEvent> = _sessionEvents.asSharedFlow()

    private val _userData = MutableStateFlow<UserData?>(null)
    val userData: StateFlow<UserData?> = _userData.asStateFlow()

    val authTokenFlow = secureTokenDataStore.data
        .map { it.authToken }

    suspend fun getAuthTokens(): AuthTokens {
        return secureTokenDataStore.data.first()
    }

    suspend fun saveAuthTokens(authToken: String, refreshToken: String) {
        secureTokenDataStore.updateData {
            it.copy(
                authToken = authToken,
                refreshToken = refreshToken
            )
        }
        // Parse and update user data
        _userData.value = jwtManager.parseToken(authToken)
        _authStatus.tryEmit(AuthStatus.AUTHENTICATED)
    }

    /**
     * Get current user data from the access token
     * Returns null if no token or token is invalid
     */
    fun getCurrentUserData(): UserData? {
        return _userData.value
    }

    /**
     * Refresh user data from stored token
     */
    suspend fun refreshUserData() {
        val tokens = getAuthTokens()
        _userData.value = jwtManager.parseToken(tokens.authToken)
    }

    fun clearSession() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            secureTokenDataStore.updateData { AuthTokens() }
            _userData.value = null
            _authStatus.emit(AuthStatus.UNAUTHENTICATED)
            _sessionEvents.emit(SessionEvent.SessionExpired)
        }
    }

    /**
     * Notify that refresh token is missing
     * This should trigger UI notification to the user
     */
    fun notifyRefreshTokenMissing() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            _sessionEvents.emit(SessionEvent.RefreshTokenMissing)
        }
    }

    fun clearToken() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            secureTokenDataStore.updateData { currentTokens ->
                currentTokens.copy(authToken = "")
            }
        }
    }

    fun clearRefreshToken() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope.launch {
            secureTokenDataStore.updateData { currentTokens ->
                currentTokens.copy(refreshToken = "")
            }
        }
    }

    fun setOfflineState() {
        if (_authStatus.value == AuthStatus.AUTHENTICATED) {
            _authStatus.tryEmit(AuthStatus.OFFLINE_AUTHENTICATED)
        }
    }

    fun setOnlineState() {
        if (_authStatus.value == AuthStatus.OFFLINE_AUTHENTICATED) {
            _authStatus.tryEmit(AuthStatus.AUTHENTICATED)
        }
    }

    private fun getInitialAuthStatus(): AuthStatus {
        return AuthStatus.UNAUTHENTICATED;
    }

    private suspend fun checkTokensExistence(): Boolean {
        val tokens = secureTokenDataStore.data.first()
        return tokens.authToken.isNotBlank()
    }

    fun loadInitialAuthStatus() {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            val tokens = secureTokenDataStore.data.first()
            val hasToken = tokens.authToken.isNotBlank()
            if (hasToken) {
                // Parse user data from token
                _userData.value = jwtManager.parseToken(tokens.authToken)
                _authStatus.emit(AuthStatus.AUTHENTICATED)
            } else {
                _userData.value = null
                _authStatus.emit(AuthStatus.UNAUTHENTICATED)
            }
        }
    }

    fun getRefreshTokenSync(): String? = runBlocking {
        val tokens = secureTokenDataStore.data.first()
        return@runBlocking tokens.refreshToken.takeIf { it.isNotBlank() }
    }

    fun getAuthTokenSync(): String? = runBlocking {
        val tokens = secureTokenDataStore.data.first()
        return@runBlocking tokens.authToken.takeIf { it.isNotBlank() }
    }
}