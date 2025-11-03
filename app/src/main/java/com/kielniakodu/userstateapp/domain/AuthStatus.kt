package com.kielniakodu.userstateapp.domain

sealed class AuthStatus {
    object AUTHENTICATED : AuthStatus()
    object UNAUTHENTICATED : AuthStatus()
    object OFFLINE_AUTHENTICATED : AuthStatus()
}