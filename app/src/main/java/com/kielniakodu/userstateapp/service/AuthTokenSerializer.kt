package com.kielniakodu.userstateapp.service

import androidx.datastore.core.Serializer
import com.google.gson.Gson
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

data class AuthTokens(
    val authToken: String = "",
    val refreshToken: String = ""
)

class AuthTokenSerializer @Inject constructor(
    private val cryptoManager: CryptoManager
) : Serializer<AuthTokens> {

    private val gson = Gson()
    override val defaultValue: AuthTokens = AuthTokens()

    override suspend fun readFrom(input: InputStream): AuthTokens {
        return try {
            val decryptedBytes = cryptoManager.decrypt(input)
            val jsonString = decryptedBytes.toString(Charsets.UTF_8)

            // Use JSON deserialization for robust parsing
            gson.fromJson(jsonString, AuthTokens::class.java) ?: defaultValue
        } catch (e: Exception) {
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: AuthTokens, output: OutputStream) {
        // Serialize to JSON for safe storage
        val jsonString = gson.toJson(t)
        cryptoManager.encrypt(jsonString.toByteArray(Charsets.UTF_8), output)
    }
}