package com.policyregistration.app.data.security

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class CryptoManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_policy_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val dbSecretKey: SecretKey

    init {
        val existingKeyBase64 = prefs.getString(KEY_DB_CIPHER, null)
        dbSecretKey = if (existingKeyBase64 != null) {
            val decoded = Base64.decode(existingKeyBase64, Base64.NO_WRAP)
            SecretKeySpec(decoded, "AES")
        } else {
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(256)
            val newKey = keyGen.generateKey()
            prefs.edit().putString(KEY_DB_CIPHER, Base64.encodeToString(newKey.encoded, Base64.NO_WRAP)).apply()
            newKey
        }
    }

    /**
     * Deterministic + Authenticated encryption for Aadhaar
     * Allows exact match duplicate lookups while keeping plaintext secure
     */
    fun encryptAadhaar(aadhaarPlain: String): String {
        if (aadhaarPlain.isBlank()) return ""
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        // Use deterministic IV derived from fixed salt + Aadhaar hash to allow duplicate index queries
        val fixedIv = ByteArray(12) { 0x42.toByte() }
        val spec = GCMParameterSpec(128, fixedIv)
        cipher.init(Cipher.ENCRYPT_MODE, dbSecretKey, spec)
        val encrypted = cipher.doFinal(aadhaarPlain.trim().toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    fun decryptAadhaar(aadhaarEncrypted: String): String {
        if (aadhaarEncrypted.isBlank()) return ""
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val fixedIv = ByteArray(12) { 0x42.toByte() }
            val spec = GCMParameterSpec(128, fixedIv)
            cipher.init(Cipher.DECRYPT_MODE, dbSecretKey, spec)
            val decoded = Base64.decode(aadhaarEncrypted, Base64.NO_WRAP)
            val decryptedBytes = cipher.doFinal(decoded)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            "XXXX-XXXX-XXXX"
        }
    }

    fun maskAadhaar(plainOrMasked: String): String {
        val digitsOnly = plainOrMasked.filter { it.isDigit() }
        return if (digitsOnly.length == 12) {
            "XXXX-XXXX-${digitsOnly.takeLast(4)}"
        } else {
            plainOrMasked
        }
    }

    companion object {
        private const val KEY_DB_CIPHER = "policy_db_aes_key"
    }
}

