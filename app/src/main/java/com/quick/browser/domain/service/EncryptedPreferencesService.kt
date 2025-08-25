package com.quick.browser.domain.service

import android.content.Context
import com.quick.browser.data.local.EncryptedPreferences

/**
 * Service for handling encrypted preferences operations
 * This service provides a clean interface for encrypted data storage
 */
class EncryptedPreferencesService(context: Context, preferencesName: String) {
    
    private val encryptedPreferences = EncryptedPreferences.getInstance(context, preferencesName)
    
    /**
     * Put a string value in encrypted preferences
     *
     * @param key The preference key
     * @param value The value to store
     */
    fun putString(key: String, value: String?) {
        encryptedPreferences.putString(key, value)
    }
    
    /**
     * Get a string value from encrypted preferences
     *
     * @param key The preference key
     * @param defaultValue The default value to return if key not found
     * @return The decrypted value or default value
     */
    fun getString(key: String, defaultValue: String?): String? {
        return encryptedPreferences.getString(key, defaultValue)
    }
    
    /**
     * Put a boolean value in encrypted preferences
     *
     * @param key The preference key
     * @param value The value to store
     */
    fun putBoolean(key: String, value: Boolean) {
        encryptedPreferences.putBoolean(key, value)
    }
    
    /**
     * Get a boolean value from encrypted preferences
     *
     * @param key The preference key
     * @param defaultValue The default value to return if key not found
     * @return The decrypted value or default value
     */
    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return encryptedPreferences.getBoolean(key, defaultValue)
    }
    
    /**
     * Put an integer value in encrypted preferences
     *
     * @param key The preference key
     * @param value The value to store
     */
    fun putInt(key: String, value: Int) {
        encryptedPreferences.putInt(key, value)
    }
    
    /**
     * Get an integer value from encrypted preferences
     *
     * @param key The preference key
     * @param defaultValue The default value to return if key not found
     * @return The decrypted value or default value
     */
    fun getInt(key: String, defaultValue: Int): Int {
        return encryptedPreferences.getInt(key, defaultValue)
    }
    
    /**
     * Put a set of strings in encrypted preferences
     *
     * @param key The preference key
     * @param value The set of strings to store
     */
    fun putStringSet(key: String, value: Set<String>?) {
        encryptedPreferences.putStringSet(key, value)
    }
    
    /**
     * Get a set of strings from encrypted preferences
     *
     * @param key The preference key
     * @param defaultValue The default value to return if key not found
     * @return The decrypted set of strings or default value
     */
    fun getStringSet(key: String, defaultValue: Set<String>?): Set<String>? {
        return encryptedPreferences.getStringSet(key, defaultValue)
    }
    
    /**
     * Remove a value from encrypted preferences
     *
     * @param key The preference key to remove
     */
    fun remove(key: String) {
        encryptedPreferences.remove(key)
    }
    
    /**
     * Clear all values from encrypted preferences
     */
    fun clear() {
        encryptedPreferences.clear()
    }
}