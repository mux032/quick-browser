package com.qb.browser.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

/**
 * Utility class for downloading NLP models
 */
class ModelDownloader(private val context: Context) {

    companion object {
        private const val TAG = "ModelDownloader"
        private const val SENTENCE_MODEL_FILENAME = "en-sent.bin"
        
        // URL to download the model from
        // This should be updated with the actual model URL
        private const val SENTENCE_MODEL_URL = "https://opennlp.sourceforge.net/models-1.5/en-sent.bin"
    }
    
    /**
     * Checks if the sentence model is already downloaded
     */
    fun isSentenceModelDownloaded(): Boolean {
        val modelFile = File(context.getExternalFilesDir(null), SENTENCE_MODEL_FILENAME)
        return modelFile.exists() && modelFile.length() > 0
    }
    
    /**
     * Downloads the sentence model if it's not already downloaded
     * @return true if the model is available (either downloaded now or previously)
     */
    suspend fun ensureSentenceModelAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isSentenceModelDownloaded()) {
                Log.d(TAG, "Sentence model already downloaded")
                return@withContext true
            }
            
            Log.d(TAG, "Downloading sentence model...")
            
            // Create a temporary file
            val tempFile = File(context.cacheDir, "$SENTENCE_MODEL_FILENAME.tmp")
            if (tempFile.exists()) {
                tempFile.delete()
            }
            
            // Download the model
            val url = URL(SENTENCE_MODEL_URL)
            val connection = url.openConnection()
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            
            connection.getInputStream().use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            // Move to final location
            val modelFile = File(context.getExternalFilesDir(null), SENTENCE_MODEL_FILENAME)
            tempFile.renameTo(modelFile)
            
            Log.d(TAG, "Sentence model downloaded successfully")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading sentence model", e)
            
            // Check if the asset exists
            val assetList = context.assets.list("")
            val hasAsset = assetList?.contains(SENTENCE_MODEL_FILENAME) == true
            
            if (hasAsset) {
                try {
                    // Check if the asset has content
                    val assetFileDescriptor = context.assets.openFd(SENTENCE_MODEL_FILENAME)
                    val hasContent = assetFileDescriptor.length > 0
                    assetFileDescriptor.close()
                    
                    if (hasContent) {
                        val modelFile = File(context.getExternalFilesDir(null), SENTENCE_MODEL_FILENAME)
                        context.assets.open(SENTENCE_MODEL_FILENAME).use { input ->
                            modelFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                        Log.d(TAG, "Used fallback sentence model from assets")
                        return@withContext true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking asset content", e)
                }
            }
            
            return@withContext false
        }
    }
}