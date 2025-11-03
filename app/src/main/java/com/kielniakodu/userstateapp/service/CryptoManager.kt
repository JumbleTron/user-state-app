package com.kielniakodu.userstateapp.service

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoManager @Inject constructor() {

    private val KEY_ALIAS = "secure_token_key_alias"
    private val TRANSFORMATION = "AES/GCM/NoPadding"

    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply {
        load(null)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existingKey != null) {
            return existingKey.secretKey
        }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val keySpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true) // Wymagane dla GCM
            .build()

        keyGenerator.init(keySpec)

        return keyGenerator.generateKey()
    }

    fun encrypt(dataToEncrypt: ByteArray, outputStream: OutputStream): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        }
        val iv = cipher.iv
        outputStream.write(iv.size)
        outputStream.write(iv)

        val encryptedBytes = cipher.doFinal(dataToEncrypt)
        outputStream.write(encryptedBytes)

        return iv + encryptedBytes
    }

    fun decrypt(inputStream: InputStream): ByteArray {
        val ivSize = inputStream.read()
        if (ivSize < 0) throw Exception("Nieprawidłowa długość IV")

        val iv = ByteArray(ivSize)
        inputStream.read(iv)

        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), javax.crypto.spec.GCMParameterSpec(128, iv))
        }

        return cipher.doFinal(inputStream.readBytes())
    }
}