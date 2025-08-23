package com.quick.browser.manager

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypted SharedPreferences wrapper using Android Keystore
 *
 * This class provides a secure way to store sensitive data using
 * Android's Keystore system for encryption.
 */
class EncryptedPreferences private constructor(context: Context, private val preferencesName: String) {
    
    companion object {
        private const val TAG = "EncryptedPreferences"
        private const val KEY_ALIAS = "quick_browser_encryption_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
        
        private var instance: EncryptedPreferences? = null
        
        /**
         * Get the singleton instance of EncryptedPreferences
         *
         * @param context The application context
         * @param preferencesName The name of the preferences file
         * @return The EncryptedPreferences instance
         */
        fun getInstance(context: Context, preferencesName: String): EncryptedPreferences {
            if (instance == null) {
                instance = EncryptedPreferences(context, preferencesName)
            }
            return instance!!
        }
    }
    
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    
    init {
        keyStore.load(null)
        generateKeyIfNotExists()
    }
    
    /**
     * Generate encryption key if it doesn't exist
     */
    private fun generateKeyIfNotExists() {
        try {
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
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
            Log.e(TAG, "Error generating encryption key", e)
        }
    }
    
    /**
     * Get the secret key from the keystore
     *
     * @return The secret key
     */
    private fun getSecretKey(): SecretKey {
        return keyStore.getKey(KEY_ALIAS, null) as SecretKey
    }
    
    /**
     * Encrypt a string value
     *
     * @param value The value to encrypt
     * @return The encrypted value as a base64 string, or null if encryption failed
     */
    private fun encrypt(value: String): String? {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            
            val iv = cipher.iv
            val encryptedBytes = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
            
            // Combine IV and encrypted data
            val combined = ByteArray(iv.size + encryptedBytes.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(encryptedBytes, 0, combined, iv.size, encryptedBytes.size)
            
            Base64.encodeToString(combined, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Error encrypting value", e)
            null
        }
    }
    
    /**
     * Decrypt a string value
     *
     * @param encryptedValue The encrypted value as a base64 string
     * @return The decrypted value, or null if decryption failed
     */
    private fun decrypt(encryptedValue: String): String? {
        return try {
            val combined = Base64.decode(encryptedValue, Base64.DEFAULT)
            
            // Extract IV and encrypted data
            val iv = ByteArray(GCM_IV_LENGTH)
            val encryptedBytes = ByteArray(combined.size - GCM_IV_LENGTH)
            
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, encryptedBytes, 0, encryptedBytes.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            
            val decryptedBytes = cipher.doFinal(encryptedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Error decrypting value", e)
            null
        }
    }
    
    /**
     * Put a string value in encrypted preferences
     *
     * @param key The preference key
     * @param value The value to store
     */
    fun putString(key: String, value: String?) {
        if (value == null) {
            sharedPreferences.edit().remove(key).apply()
            return
        }
        
        val encryptedValue = encrypt(value)
        if (encryptedValue != null) {
            sharedPreferences.edit().putString(key, encryptedValue).apply()
        }
    }
    
    /**
     * Get a string value from encrypted preferences
     *
     * @param key The preference key
     * @param defaultValue The default value to return if key not found
     * @return The decrypted value or default value
     */
    fun getString(key: String, defaultValue: String?): String? {
        val encryptedValue = sharedPreferences.getString(key, null)
        return if (encryptedValue != null) {
            decrypt(encryptedValue) ?: defaultValue
        } else {
            defaultValue
        }
    }
    
    /**
     * Put a boolean value in encrypted preferences
     *
     * @param key The preference key
     * @param value The value to store
     */
    fun putBoolean(key: String, value: Boolean) {
        putString(key, value.toString())
    }
    
    /**
     * Get a boolean value from encrypted preferences
     *
     * @param key The preference key
     * @param defaultValue The default value to return if key not found
     * @return The decrypted value or default value
     */
    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        val stringValue = getString(key, defaultValue.toString())
        return try {
            stringValue?.toBoolean() ?: defaultValue
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing boolean value", e)
            defaultValue
        }
    }
    
    /**
     * Put an integer value in encrypted preferences
     *
     * @param key The preference key
     * @param value The value to store
     */
    fun putInt(key: String, value: Int) {
        putString(key, value.toString())
    }
    
    /**
     * Get an integer value from encrypted preferences
     *
     * @param key The preference key
     * @param defaultValue The default value to return if key not found
     * @return The decrypted value or default value
     */
    fun getInt(key: String, defaultValue: Int): Int {
        val stringValue = getString(key, defaultValue.toString())
        return try {
            stringValue?.toInt() ?: defaultValue
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing integer value", e)
            defaultValue
        }
    }
    
    /**
     * Put a set of strings in encrypted preferences
     *
     * @param key The preference key
     * @param value The set of strings to store
     */
    fun putStringSet(key: String, value: Set<String>?) {
        if (value == null) {
            sharedPreferences.edit().remove(key).apply()
            return
        }
        
        // Convert set to JSON string for storage
        val jsonString = value.joinToString(",") { it }
        putString(key, jsonString)
    }
    
    /**
     * Get a set of strings from encrypted preferences
     *
     * @param key The preference key
     * @param defaultValue The default value to return if key not found
     * @return The decrypted set of strings or default value
     */
    fun getStringSet(key: String, defaultValue: Set<String>?): Set<String>? {
        val jsonString = getString(key, null)
        return if (jsonString != null) {
            if (jsonString.isEmpty()) {
                emptySet()
            } else {
                jsonString.split(",").toSet()
            }
        } else {
            defaultValue
        }
    }
    
    /**
     * Remove a value from encrypted preferences
     *
     * @param key The preference key to remove
     */
    fun remove(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }
    
    /**
     * Clear all values from encrypted preferences
     */
    fun clear() {
        sharedPreferences.edit().clear().apply()
    }
}