package com.qb.browser.util

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale
import java.util.UUID

/**
 * Manager for text-to-speech functionality
 */
class TextToSpeechManager(private val context: Context) {
    
    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false
    private var isSpeaking = false
    private var currentUtteranceId = ""
    
    private var speechRate = 1.0f
    private var pitch = 1.0f
    private var selectedVoice: Voice? = null
    private var locale = Locale.getDefault()
    
    interface TtsCallback {
        fun onInitialized(status: Boolean)
        fun onStart()
        fun onDone()
        fun onError(errorMessage: String)
    }
    
    private var callback: TtsCallback? = null
    
    companion object {
        private const val TAG = "TextToSpeechManager"
        
        const val MIN_SPEECH_RATE = 0.5f
        const val MAX_SPEECH_RATE = 2.0f
        const val DEFAULT_SPEECH_RATE = 1.0f
        
        const val MIN_PITCH = 0.5f
        const val MAX_PITCH = 2.0f
        const val DEFAULT_PITCH = 1.0f
    }
    
    /**
     * Initialize the text-to-speech engine
     */
    fun initialize(callback: TtsCallback) {
        this.callback = callback
        
        // Ensure we don't initialize twice
        if (isInitialized) {
            callback.onInitialized(true)
            return
        }
        
        try {
            textToSpeech = TextToSpeech(context) { status ->
                isInitialized = status == TextToSpeech.SUCCESS
                
                if (isInitialized) {
                    // Set default locale and configurations
                    val result = textToSpeech?.setLanguage(locale)
                    
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e(TAG, "Language not supported: ${locale.language}")
                        callback.onError("Language not supported: ${locale.language}")
                        isInitialized = false
                    } else {
                        // Set default pitch and rate
                        textToSpeech?.setPitch(pitch)
                        textToSpeech?.setSpeechRate(speechRate)
                        
                        // Set utterance progress listener
                        setupProgressListener()
                    }
                } else {
                    Log.e(TAG, "Failed to initialize Text-to-Speech")
                    callback.onError("Failed to initialize Text-to-Speech")
                }
                
                callback.onInitialized(isInitialized)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Text-to-Speech: ${e.message}")
            callback.onError("Error initializing Text-to-Speech: ${e.message}")
            isInitialized = false
        }
    }
    
    /**
     * Set up the utterance progress listener to track speaking progress
     */
    private fun setupProgressListener() {
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) {
                isSpeaking = true
                callback?.onStart()
            }
            
            override fun onDone(utteranceId: String) {
                if (utteranceId == currentUtteranceId) {
                    isSpeaking = false
                    callback?.onDone()
                }
            }
            
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                if (utteranceId == currentUtteranceId) {
                    isSpeaking = false
                    callback?.onError("Error during speech")
                }
            }
            
            override fun onError(utteranceId: String, errorCode: Int) {
                super.onError(utteranceId, errorCode)
                if (utteranceId == currentUtteranceId) {
                    isSpeaking = false
                    callback?.onError("Error during speech, code: $errorCode")
                }
            }
        })
    }
    
    /**
     * Speak the given text
     */
    fun speak(text: String): Boolean {
        if (!isInitialized) {
            Log.e(TAG, "Text-to-Speech not initialized")
            callback?.onError("Text-to-Speech not initialized")
            return false
        }
        
        if (text.isEmpty()) {
            Log.e(TAG, "Empty text provided")
            callback?.onError("Empty text provided")
            return false
        }
        
        try {
            // Stop any ongoing speech
            stop()
            
            // Generate a unique utterance ID
            currentUtteranceId = UUID.randomUUID().toString()
            
            // Split text into manageable chunks if it's too long
            val chunks = splitTextIntoChunks(text)
            
            // Speak the first chunk
            if (chunks.isNotEmpty()) {
                speakTextChunk(chunks[0], currentUtteranceId)
                return true
            }
            
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Error speaking text: ${e.message}")
            callback?.onError("Error speaking text: ${e.message}")
            return false
        }
    }
    
    /**
     * Speak a chunk of text
     */
    private fun speakTextChunk(textChunk: String, utteranceId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textToSpeech?.speak(textChunk, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        } else {
            @Suppress("DEPRECATION")
            textToSpeech?.speak(textChunk, TextToSpeech.QUEUE_FLUSH, HashMap<String, String>().apply {
                put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            })
        }
    }
    
    /**
     * Split text into manageable chunks for TTS
     */
    private fun splitTextIntoChunks(text: String): List<String> {
        // Maximum characters per chunk
        val maxChunkSize = 4000
        
        return if (text.length <= maxChunkSize) {
            listOf(text)
        } else {
            val chunks = mutableListOf<String>()
            var start = 0
            
            while (start < text.length) {
                var end = minOf(start + maxChunkSize, text.length)
                
                // Try to find a natural breakpoint (end of sentence, paragraph)
                if (end < text.length) {
                    // Look for period, question mark, exclamation point followed by space or newline
                    val periodIndex = text.lastIndexOfAny(charArrayOf('.', '?', '!'), end)
                    if (periodIndex > start && periodIndex < end) {
                        end = periodIndex + 1
                    }
                }
                
                chunks.add(text.substring(start, end))
                start = end
            }
            
            chunks
        }
    }
    
    /**
     * Stop speaking
     */
    fun stop() {
        if (isInitialized && isSpeaking) {
            textToSpeech?.stop()
            isSpeaking = false
        }
    }
    
    /**
     * Check if currently speaking
     */
    fun isSpeaking(): Boolean {
        return isSpeaking
    }
    
    /**
     * Set speech rate
     */
    fun setSpeechRate(rate: Float) {
        val boundedRate = rate.coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE)
        this.speechRate = boundedRate
        textToSpeech?.setSpeechRate(boundedRate)
    }
    
    /**
     * Set pitch
     */
    fun setPitch(pitch: Float) {
        val boundedPitch = pitch.coerceIn(MIN_PITCH, MAX_PITCH)
        this.pitch = boundedPitch
        textToSpeech?.setPitch(boundedPitch)
    }
    
    /**
     * Set language
     */
    fun setLanguage(locale: Locale): Boolean {
        if (!isInitialized) return false
        
        val result = textToSpeech?.setLanguage(locale)
        val success = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        
        if (success) {
            this.locale = locale
        } else {
            Log.e(TAG, "Language not supported: ${locale.language}")
            callback?.onError("Language not supported: ${locale.language}")
        }
        
        return success
    }
    
    /**
     * Get available languages
     */
    fun getAvailableLanguages(): Set<Locale> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textToSpeech?.availableLanguages ?: emptySet()
        } else {
            val locales = mutableSetOf<Locale>()
            Locale.getAvailableLocales().forEach { locale ->
                val result = textToSpeech?.isLanguageAvailable(locale)
                if (result == TextToSpeech.LANG_AVAILABLE || result == TextToSpeech.LANG_COUNTRY_AVAILABLE || result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE) {
                    locales.add(locale)
                }
            }
            locales
        }
    }
    
    /**
     * Release resources
     */
    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        isInitialized = false
        isSpeaking = false
        callback = null
    }
}