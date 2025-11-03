package com.kielniakodu.userstateapp.service

import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject

class TokenAuthenticator @Inject constructor(
    private val sessionManager: SessionManager,
    private val apiService: ApiService
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        val currentAuthToken = sessionManager.getAuthTokenSync()

        val requestAuthToken = response.request.header("Authorization")?.removePrefix("Bearer ")
        if (currentAuthToken != null && currentAuthToken != requestAuthToken) {
            return response.request.newBuilder()
                .header("Authorization", "Bearer $currentAuthToken")
                .build()
        }

        return synchronized(this) {
            val refreshToken = sessionManager.getRefreshTokenSync()
            if (refreshToken.isNullOrBlank()) {
                // CASE 1: No refresh token. Notify user and logout.
                sessionManager.notifyRefreshTokenMissing()
                runBlocking { sessionManager.clearSession() }
                return null
            }

            // Ponownie sprawdzamy, czy inny wątek nie odświeżył w międzyczasie (po wyjściu z poprzedniego bloku)
            val checkAuthToken = sessionManager.getAuthTokenSync()
            if (checkAuthToken != null && checkAuthToken != requestAuthToken) {
                // Inny wątek już odświeżył.
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $checkAuthToken")
                    .build()
            }

            // 3. Właściwe odświeżanie tokena
            val refreshResult = runBlocking {
                try {
                    val refreshResponse = apiService.refreshToken(RefreshTokenRequest(refreshToken))
                    if (refreshResponse.isSuccessful && refreshResponse.body() != null) {
                        val newTokens = refreshResponse.body()!!
                        sessionManager.saveAuthTokens(newTokens.token, newTokens.refreshToken)
                        return@runBlocking newTokens
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                return@runBlocking null
            }

            if (refreshResult != null) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer ${refreshResult.token}")
                    .build()
            } else {
                runBlocking { sessionManager.clearSession() }
                return null
            }
        }
    }
}