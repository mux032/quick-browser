package com.quick.browser.domain.service

import android.content.Context
import com.quick.browser.data.network.ModelDownloader

/**
 * Service for handling model downloading operations
 * This service provides a clean interface for downloading NLP models
 */
class ModelDownloadService(context: Context) {
    
    private val modelDownloader = ModelDownloader(context)
    
    /**
     * Checks if the sentence model is already downloaded
     */
    fun isSentenceModelDownloaded(): Boolean {
        return modelDownloader.isSentenceModelDownloaded()
    }

    /**
     * Downloads the sentence model if it's not already downloaded
     * @return true if the model is available (either downloaded now or previously)
     */
    suspend fun ensureSentenceModelAvailable(): Boolean {
        return modelDownloader.ensureSentenceModelAvailable()
    }
}