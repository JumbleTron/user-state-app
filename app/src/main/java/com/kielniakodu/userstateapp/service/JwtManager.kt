package com.kielniakodu.userstateapp.service

import android.util.Log
import com.auth0.android.jwt.JWT
import com.kielniakodu.userstateapp.domain.UserData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JwtManager @Inject constructor() {

    /**
     * Parses JWT token and extracts user data
     * Returns null if token is invalid or expired
     */
    fun parseToken(token: String): UserData? {
        return try {
            if (token.isBlank()) return null

            val jwt = JWT(token)

            // Check if token is expired
            val expiresAt = jwt.expiresAt?.time ?: 0
            if (System.currentTimeMillis() > expiresAt) {
                Log.w("JwtManager", "Token is expired")
                return null
            }

            // Extract user data from JWT claims
            // Adjust these claim names based on your backend's JWT structure
            val userId = jwt.getClaim("sub").asString() ?: ""
            val email = jwt.getClaim("email").asString()
                ?: jwt.getClaim("username").asString()
                ?: ""
            val username = jwt.getClaim("name").asString()

            // Extract roles if present
            val roles = jwt.getClaim("roles").asArray(String::class.java)?.toList()
                ?: emptyList()

            UserData(
                userId = userId,
                email = email,
                username = username,
                roles = roles,
                expiresAt = expiresAt
            )
        } catch (e: Exception) {
            Log.e("JwtManager", "Failed to parse JWT token", e)
            null
        }
    }

    /**
     * Checks if token is expired without parsing full user data
     */
    fun isTokenExpired(token: String): Boolean {
        return try {
            if (token.isBlank()) return true
            val jwt = JWT(token)
            jwt.isExpired(0) // 0 seconds leeway
        } catch (e: Exception) {
            true
        }
    }

    /**
     * Gets expiration time in milliseconds
     */
    fun getExpirationTime(token: String): Long? {
        return try {
            if (token.isBlank()) return null
            val jwt = JWT(token)
            jwt.expiresAt?.time
        } catch (e: Exception) {
            null
        }
    }
}