package com.quick.browser.manager

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.util.concurrent.atomic.AtomicBoolean
import opennlp.tools.tokenize.SimpleTokenizer
import opennlp.tools.sentdetect.SentenceDetectorME
import opennlp.tools.sentdetect.SentenceModel
import java.io.InputStream

/**
 * Manager class for handling article summarization using NLP techniques
 */
class SummarizationManager constructor(private val context: Context) {

    companion object {
        private const val TAG = "SummarizationManager"
        private const val MAX_SUMMARY_POINTS = 10
        private const val MIN_SUMMARY_POINTS = 5
    }

    private val tokenizer = SimpleTokenizer.INSTANCE
    private var sentenceDetector: SentenceDetectorME? = null
    private val isModelLoaded = AtomicBoolean(false)

    /**
     * Loads the sentence detection model
     */
    private suspend fun loadModel(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (isModelLoaded.get()) {
                return@withContext true
            }

            // Try to load the sentence model from assets
            try {
                val inputStream: InputStream = context.assets.open("en-sent.bin")
                val model = SentenceModel(inputStream)
                sentenceDetector = SentenceDetectorME(model)
                isModelLoaded.set(true)
                Log.d(TAG, "Sentence model loaded successfully")
                return@withContext true
            } catch (e: Exception) {
                Log.e(TAG, "Error loading sentence model, using fallback", e)
                // We'll use a simple regex-based approach as fallback
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in loadModel", e)
            return@withContext false
        }
    }

    /**
     * Extracts the main content from a webpage
     */
    private suspend fun extractContent(html: String): String = withContext(Dispatchers.Default) {
        try {
            val doc = Jsoup.parse(html)

            // Remove non-content elements
            doc.select("script, style, nav, footer, header, aside, .ads, .comments, .sidebar, iframe, form, button")
                .remove()

            // Remove all elements with common ad-related class names
            doc.select("[class*=ad], [class*=Ad], [class*=AD], [class*=banner], [class*=popup], [class*=cookie], [id*=ad], [id*=Ad]")
                .remove()

            // Remove social media widgets
            doc.select("[class*=social], [class*=share], [class*=follow], [class*=subscribe]").remove()

            // Remove all HTML attributes that might contain unwanted information
            doc.select("*").forEach { element ->
                val tagName = element.tagName()
                if (tagName != "body" && tagName != "article" && tagName != "p" && tagName != "div") {
                    val text = element.text()
                    element.clearAttributes()
                    element.text(text)
                }
            }

            // Get the main content
            // Clean up the text
            return@withContext doc.text()
                .replace("\\s+".toRegex(), " ")
                .replace("\\[[0-9]+\\]".toRegex(), "") // Remove citation numbers like [1], [2], etc.
                .replace("\\(https?://[^\\s]+\\)".toRegex(), "") // Remove URLs in parentheses
                .trim()
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting content", e)
            return@withContext ""
        }
    }

    /**
     * Splits text into sentences using the loaded model or a fallback approach
     */
    private fun detectSentences(text: String): Array<String> {
        return if (isModelLoaded.get() && sentenceDetector != null) {
            // Use the OpenNLP sentence detector
            sentenceDetector!!.sentDetect(text)
        } else {
            // Fallback: simple regex-based sentence detection
            text.split("(?<=[.!?])\\s+".toRegex()).toTypedArray()
        }
    }

    /**
     * Calculates the importance score of a sentence based on various factors
     */
    private fun calculateSentenceScore(sentence: String, position: Int, totalSentences: Int): Double {
        // Position score - sentences at the beginning are often more important
        val positionScore = 1.0 - (position.toDouble() / totalSentences.coerceAtLeast(1))

        // Length score - very short or very long sentences are less likely to be good summary points
        val lengthScore = when {
            sentence.length < 20 -> 0.3
            sentence.length in 20..50 -> 0.7
            sentence.length in 51..150 -> 1.0
            sentence.length in 151..200 -> 0.7
            else -> 0.4  // Penalize very long sentences more
        }

        // Word count score - sentences with more words often contain more information
        val wordCount = tokenizer.tokenize(sentence).size
        val wordCountScore = when {
            wordCount < 5 -> 0.3
            wordCount in 5..10 -> 0.7
            wordCount in 11..20 -> 1.0
            wordCount in 21..30 -> 0.7
            else -> 0.4  // Penalize very wordy sentences
        }

        // Presence of numbers often indicates important facts
        val containsNumbers = sentence.contains("\\d+".toRegex())
        val numberScore = if (containsNumbers) 0.2 else 0.0

        // Penalize sentences that might be incomplete or contain HTML artifacts
        val htmlArtifactScore = if (
            sentence.contains("<") ||
            sentence.contains(">") ||
            sentence.contains("&nbsp;") ||
            sentence.contains("&lt;") ||
            sentence.contains("&gt;") ||
            sentence.contains("javascript:") ||
            sentence.contains("\\{\\{".toRegex()) ||
            sentence.contains("\\}\\}".toRegex()) ||
            sentence.contains("undefined") ||
            sentence.contains("null,") ||
            sentence.contains("NaN")
        ) -0.5 else 0.0

        // Penalize sentences that are likely navigation or UI text
        val navigationTextScore = if (
            sentence.contains("click here", ignoreCase = true) ||
            sentence.contains("read more", ignoreCase = true) ||
            sentence.contains("learn more", ignoreCase = true) ||
            sentence.contains("sign up", ignoreCase = true) ||
            sentence.contains("log in", ignoreCase = true) ||
            sentence.contains("subscribe", ignoreCase = true) ||
            sentence.contains("cookie", ignoreCase = true) ||
            sentence.contains("privacy policy", ignoreCase = true) ||
            sentence.contains("terms of service", ignoreCase = true) ||
            sentence.contains("copyright", ignoreCase = true)
        ) -0.5 else 0.0

        // Calculate final score
        return positionScore * 0.4 +
                lengthScore * 0.3 +
                wordCountScore * 0.2 +
                numberScore * 0.1 +
                htmlArtifactScore +
                navigationTextScore
    }

    /**
     * Summarizes the given HTML content into bullet points
     */
    suspend fun summarizeContent(html: String): List<String> = withContext(Dispatchers.Default) {
        try {
            // Extract the main content from the HTML
            val content = extractContent(html)
            if (content.isBlank() || content.length < 100) {
                Log.d(TAG, "Content too short or empty")
                return@withContext emptyList()
            }

            // Try to load the model, but continue with fallback if it fails
            val modelLoaded = try {
                loadModel()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading model, using fallback", e)
                false
            }

            Log.d(TAG, "Using extractive summarization" + if (modelLoaded) " with NLP model" else " (fallback)")

            // Split the content into sentences
            val sentences = detectSentences(content)
                .filter { it.length > 20 && it.length < 300 } // Filter out very short or very long sentences
                .take(MAX_SUMMARY_POINTS * 5) // Take more sentences to have a better selection

            if (sentences.isEmpty()) {
                Log.d(TAG, "No suitable sentences found")
                return@withContext emptyList()
            }

            // Score sentences based on various factors
            val scoredSentences = sentences.mapIndexed { index, sentence ->
                val score = calculateSentenceScore(sentence, index, sentences.size)
                Triple(sentence, score, index)
            }

            // Filter out sentences with negative scores (likely HTML artifacts or navigation text)
            val filteredSentences = scoredSentences.filter { it.second > 0 }

            if (filteredSentences.isEmpty()) {
                Log.d(TAG, "No sentences with positive scores found")
                return@withContext emptyList()
            }

            // Sort by score and take top N
            val summaryPoints = filteredSentences
                .sortedByDescending { it.second }
                .take(MAX_SUMMARY_POINTS * 2)
                .sortedBy { it.third } // Sort back by original position
                .map { it.first }
                .take(MAX_SUMMARY_POINTS)
                .filter { it.length > 20 }

            // Ensure we have at least MIN_SUMMARY_POINTS and at most MAX_SUMMARY_POINTS
            if (summaryPoints.size < MIN_SUMMARY_POINTS) {
                Log.d(TAG, "Not enough summary points: ${summaryPoints.size}")
                return@withContext emptyList()
            }

            // Limit to MAX_SUMMARY_POINTS (10) to ensure we don't have too many
            val limitedPoints = if (summaryPoints.size > MAX_SUMMARY_POINTS) {
                summaryPoints.take(MAX_SUMMARY_POINTS)
            } else {
                summaryPoints
            }

            Log.d(TAG, "Final summary points count: ${limitedPoints.size}")

            // Format and clean the bullet points
            val bulletPoints = limitedPoints.map { sentence ->
                // Clean the sentence
                var cleanedSentence = sentence
                    .replace("\\s+".toRegex(), " ") // Normalize whitespace
                    .replace("&nbsp;", " ")
                    .replace("&amp;", "&")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&quot;", "\"")
                    .replace("&#39;", "'")
                    .replace("<[^>]*>".toRegex(), "") // Remove any remaining HTML tags
                    .replace("\\{\\{[^}]*\\}\\}".toRegex(), "") // Remove template expressions
                    .replace("\\[[0-9]+\\]".toRegex(), "") // Remove citation numbers
                    .trim()

                // Truncate very long sentences
                if (cleanedSentence.length > 200) {
                    val truncated = cleanedSentence.substring(0, 197) + "..."
                    cleanedSentence = truncated
                }

                // Ensure the sentence starts with a capital letter
                if (cleanedSentence.isNotEmpty() && cleanedSentence[0].isLowerCase()) {
                    cleanedSentence = cleanedSentence[0].uppercaseChar() + cleanedSentence.substring(1)
                }

                // Add period if missing
                if (!cleanedSentence.endsWith(".") && !cleanedSentence.endsWith("!") && !cleanedSentence.endsWith("?")) {
                    cleanedSentence = "$cleanedSentence."
                }

                cleanedSentence
            }

            // Filter out any duplicate or nearly duplicate sentences
            val uniqueBulletPoints = mutableListOf<String>()
            for (point in bulletPoints) {
                val isDuplicate = uniqueBulletPoints.any { existing ->
                    val similarity = calculateSimilarity(existing, point)
                    similarity > 0.7 // If more than 70% similar, consider it a duplicate
                }

                if (!isDuplicate) {
                    uniqueBulletPoints.add(point)
                }
            }

            Log.d(TAG, "Generated ${uniqueBulletPoints.size} unique summary points")
            return@withContext uniqueBulletPoints.take(MAX_SUMMARY_POINTS)
        } catch (e: Exception) {
            Log.e(TAG, "Error summarizing content", e)
            return@withContext emptyList()
        }
    }

    /**
     * Calculate similarity between two strings (simple implementation)
     */
    private fun calculateSimilarity(s1: String, s2: String): Double {
        val words1 = s1.lowercase().split("\\W+".toRegex()).filter { it.length > 3 }.toSet()
        val words2 = s2.lowercase().split("\\W+".toRegex()).filter { it.length > 3 }.toSet()

        if (words1.isEmpty() || words2.isEmpty()) return 0.0

        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size

        return intersection.toDouble() / union.toDouble()
    }
}