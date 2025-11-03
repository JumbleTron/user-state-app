package com.kielniakodu.userstateapp.interceptor

import com.kielniakodu.userstateapp.service.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkStatusInterceptor @Inject constructor(
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        try {
            val response = chain.proceed(chain.request())
            sessionManager.setOnlineState()
            return response
        } catch (e: IOException) {
            sessionManager.setOfflineState()
            throw e
        }
    }
}