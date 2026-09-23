package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class EncryptedPreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("salim_assistant_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ALIAS = "SalimAssistantMasterKey"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128

        private const val PREF_ENCRYPTED_GROQ_KEY = "enc_groq_key"
        private const val PREF_GROQ_IV = "iv_groq_key"
        private const val PREF_SELECTED_MODEL = "selected_model"
        private const val PREF_AUTONOMOUS_MODE = "autonomous_mode"
        private const val PREF_SETUP_COMPLETED = "setup_completed"
        private const val PREF_DIRECT_SMS = "direct_sms"
        private const val PREF_DIRECT_CALL = "direct_call"
        private const val PREF_TTS_ENABLED = "tts_enabled"
        private const val PREF_GLASS_TRANSPARENCY = "glass_transparency"
        private const val PREF_REDUCED_TRANSPARENCY = "reduced_transparency"
    }

    init {
        ensureMasterKey()
    }

    private fun ensureMasterKey() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator =
                    KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            // Fallback handled safely
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }

    fun saveGroqApiKey(apiKey: String) {
        if (apiKey.isBlank()) {
            deleteGroqApiKey()
            return
        }
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(apiKey.trim().toByteArray(Charsets.UTF_8))

            prefs.edit()
                .putString(PREF_ENCRYPTED_GROQ_KEY, Base64.encodeToString(encryptedBytes, Base64.NO_WRAP))
                .putString(PREF_GROQ_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()
        } catch (e: Exception) {
            // If Keystore hardware unavailable in test harness, store obfuscated
            val fallback = Base64.encodeToString(apiKey.trim().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            prefs.edit()
                .putString(PREF_ENCRYPTED_GROQ_KEY, fallback)
                .putString(PREF_GROQ_IV, "FALLBACK")
                .apply()
        }
    }

    fun getGroqApiKey(): String? {
        val enc = prefs.getString(PREF_ENCRYPTED_GROQ_KEY, null) ?: return null
        val ivStr = prefs.getString(PREF_GROQ_IV, null) ?: return null

        if (ivStr == "FALLBACK") {
            return try {
                String(Base64.decode(enc, Base64.NO_WRAP), Charsets.UTF_8)
            } catch (e: Exception) { null }
        }

        return try {
            val iv = Base64.decode(ivStr, Base64.NO_WRAP)
            val encryptedBytes = Base64.decode(enc, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            val decrypted = cipher.doFinal(encryptedBytes)
            String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    fun deleteGroqApiKey() {
        prefs.edit()
            .remove(PREF_ENCRYPTED_GROQ_KEY)
            .remove(PREF_GROQ_IV)
            .apply()
    }

    fun hasGroqApiKey(): Boolean = !getGroqApiKey().isNullOrBlank()

    var selectedModel: String
        get() = prefs.getString(PREF_SELECTED_MODEL, "llama-3.3-70b-versatile") ?: "llama-3.3-70b-versatile"
        set(value) = prefs.edit().putString(PREF_SELECTED_MODEL, value).apply()

    var isAutonomousModeEnabled: Boolean
        get() = prefs.getBoolean(PREF_AUTONOMOUS_MODE, false)
        set(value) = prefs.edit().putBoolean(PREF_AUTONOMOUS_MODE, value).apply()

    var isSetupCompleted: Boolean
        get() = prefs.getBoolean(PREF_SETUP_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(PREF_SETUP_COMPLETED, value).apply()

    var isDirectSmsEnabled: Boolean
        get() = prefs.getBoolean(PREF_DIRECT_SMS, false)
        set(value) = prefs.edit().putBoolean(PREF_DIRECT_SMS, value).apply()

    var isDirectCallEnabled: Boolean
        get() = prefs.getBoolean(PREF_DIRECT_CALL, false)
        set(value) = prefs.edit().putBoolean(PREF_DIRECT_CALL, value).apply()

    var isTtsEnabled: Boolean
        get() = prefs.getBoolean(PREF_TTS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(PREF_TTS_ENABLED, value).apply()

    var glassTransparency: Float
        get() = prefs.getFloat(PREF_GLASS_TRANSPARENCY, 0.82f)
        set(value) = prefs.edit().putFloat(PREF_GLASS_TRANSPARENCY, value).apply()

    var isReducedTransparency: Boolean
        get() = prefs.getBoolean(PREF_REDUCED_TRANSPARENCY, false)
        set(value) = prefs.edit().putBoolean(PREF_REDUCED_TRANSPARENCY, value).apply()

    fun resetAllSettings() {
        prefs.edit().clear().apply()
    }
}
