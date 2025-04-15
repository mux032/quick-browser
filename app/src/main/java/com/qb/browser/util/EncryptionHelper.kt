package com.qb.browser.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Utility for encrypting and decrypting data
 */
class EncryptionHelper {
    
    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE = 256
        private const val KEY_NAME = "qb_browser_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val IV_SEPARATOR = "]"
    }
    
    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }
    
    /**
     * Get or create the encryption key
     */
    private fun getOrCreateKey(): SecretKey {
        // Check if key exists
        val existingKey = keyStore.getEntry(KEY_NAME, null) as? KeyStore.SecretKeyEntry
        
        // If key exists, return it
        if (existingKey != null) {
            return existingKey.secretKey
        }
        
        // Otherwise, generate a new key
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        
        val keyGenParameterSpec = KeyGenParameterSpec.Builder(
            KEY_NAME,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
            .build()
        
        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }
    
    /**
     * Encrypt a string
     */
    fun encrypt(plaintext: String): String {
        val key = getOrCreateKey()
        
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        
        // Combine IV and encrypted data with a separator
        val encryptedWithIv = iv + encrypted
        
        return Base64.encodeToString(encryptedWithIv, Base64.DEFAULT)
    }
    
    /**
     * Decrypt a string
     */
    fun decrypt(encryptedText: String): String {
        try {
            val key = getOrCreateKey()
            
            val encryptedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
            
            // Extract IV from the beginning (first 12 bytes for GCM)
            val iv = encryptedBytes.copyOfRange(0, 12)
            val encrypted = encryptedBytes.copyOfRange(12, encryptedBytes.size)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            
            val decrypted = cipher.doFinal(encrypted)
            return String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            // Handle decryption failures
            e.printStackTrace()
            throw e
        }
    }
    
    /**
     * Use a simpler encryption approach for older Android versions or when KeyStore is unavailable
     * This is a fallback method but is less secure.
     */
    private fun fallbackEncrypt(plaintext: String, password: String): String {
        try {
            // Simple XOR encryption with password as key
            val key = password.toByteArray()
            val input = plaintext.toByteArray()
            val output = ByteArray(input.size)
            
            for (i in input.indices) {
                output[i] = (input[i].toInt() xor key[i % key.size].toInt()).toByte()
            }
            
            return Base64.encodeToString(output, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
    
    /**
     * Fallback decryption for older Android devices
     */
    private fun fallbackDecrypt(encryptedText: String, password: String): String {
        try {
            val encryptedBytes = Base64.decode(encryptedText, Base64.DEFAULT)
            val key = password.toByteArray()
            val output = ByteArray(encryptedBytes.size)
            
            for (i in encryptedBytes.indices) {
                output[i] = (encryptedBytes[i].toInt() xor key[i % key.size].toInt()).toByte()
            }
            
            return String(output)
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
