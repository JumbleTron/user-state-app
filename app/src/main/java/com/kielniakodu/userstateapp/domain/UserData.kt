package com.kielniakodu.userstateapp.domain

data class UserData(
    val userId: String,
    val email: String,
    val username: String? = null,
    val roles: List<String> = emptyList(),
    val expiresAt: Long = 0
)