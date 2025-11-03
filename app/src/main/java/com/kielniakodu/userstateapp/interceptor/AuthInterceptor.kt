package com.kielniakodu.userstateapp.interceptor

import com.kielniakodu.userstateapp.service.SessionManager
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestBuilder = originalRequest.newBuilder()
        runBlocking {
            val token = sessionManager.getAuthTokens().authToken
            if (token.isNotEmpty()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }
        }

        val request = requestBuilder.build()

        return chain.proceed(request)
    }
}