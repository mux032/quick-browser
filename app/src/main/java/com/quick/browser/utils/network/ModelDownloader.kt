package com.quick.browser.utils.network

import android.content.Context
import com.quick.browser.utils.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

/**
 * Utility class for downloading NLP models
 *
 * This class handles downloading and managing NLP models required by the application.
 * It provides methods to check if models are available, download them if needed, and
 * manage their storage location.
 *
 * @param context The application context
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
     *
     * @return True if the sentence model is downloaded and available, false otherwise
     */
    fun isSentenceModelDownloaded(): Boolean {
        val modelFile = File(context.getExternalFilesDir(null), SENTENCE_MODEL_FILENAME)
        return modelFile.exists() && modelFile.length() > 0
    }

    /**
     * Downloads the sentence model if it's not already downloaded
     *
     * This method attempts to download the sentence model from the configured URL.
     * If downloading fails, it tries to use a fallback model from the app's assets.
     *
     * @return true if the model is available (either downloaded now or previously)
     */
    suspend fun ensureSentenceModelAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isSentenceModelDownloaded()) {
                Logger.d(TAG, "Sentence model already downloaded")
                return@withContext true
            }

            Logger.d(TAG, "Downloading sentence model...")

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

            Logger.d(TAG, "Sentence model downloaded successfully")
            return@withContext true
        } catch (e: Exception) {
            Logger.e(TAG, "Error downloading sentence model", e)

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
                        Logger.d(TAG, "Used fallback sentence model from assets")
                        return@withContext true
                    }
                } catch (e: Exception) {
                    Logger.e(TAG, "Error checking asset content", e)
                }
            }

            return@withContext false
        }
    }
}