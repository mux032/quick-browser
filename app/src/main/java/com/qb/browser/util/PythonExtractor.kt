package com.qb.browser.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader

/**
 * Utility for extracting web content using Python's trafilatura library
 */
class PythonExtractor(private val context: Context) {
    
    companion object {
        private const val TAG = "PythonExtractor"
        private const val SCRIPT_FILENAME = "extract_content.py"
    }
    
    /**
     * Data class to hold extraction results
     */
    data class ExtractionResult(
        val title: String,
        val content: String,
        val author: String,
        val date: String,
        val url: String,
        val hostname: String,
        val success: Boolean
    )
    
    /**
     * Extract content from a URL using Python's trafilatura library
     */
    suspend fun extractContent(url: String): ExtractionResult = withContext(Dispatchers.IO) {
        try {
            // Create a Python script file
            val scriptFile = File(context.cacheDir, SCRIPT_FILENAME)
            createPythonScript(scriptFile)
            
            // Execute the script with the URL
            val process = ProcessBuilder("python3", scriptFile.absolutePath, url)
                .redirectErrorStream(true)
                .start()
            
            // Read the output
            val reader = InputStreamReader(process.inputStream)
            val output = reader.readText()
            
            // Wait for process to complete
            val exitCode = process.waitFor()
            
            if (exitCode != 0) {
                Log.e(TAG, "Python process failed with exit code $exitCode: $output")
                return@withContext createEmptyResult(url)
            }
            
            // Parse the output to get the extraction result
            parseOutput(output, url)
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting content with Python: ${e.message}", e)
            createEmptyResult(url)
        }
    }
    
    /**
     * Create a Python script file with trafilatura extraction code
     */
    private fun createPythonScript(file: File) {
        try {
            val scriptContent = """
                #!/usr/bin/env python3
                import sys
                import json
                from datetime import datetime

                try:
                    import trafilatura
                    from trafilatura.metadata import extract_metadata
                except ImportError:
                    print(json.dumps({
                        "success": False,
                        "error": "Trafilatura library not found"
                    }))
                    sys.exit(1)

                def extract_content(url):
                    try:
                        # Download content
                        downloaded = trafilatura.fetch_url(url)
                        if not downloaded:
                            return {
                                "success": False,
                                "error": "Failed to download content"
                            }
                        
                        # Extract metadata
                        metadata = extract_metadata(downloaded)
                        
                        # Extract main content
                        content = trafilatura.extract(
                            downloaded,
                            include_formatting=True,
                            include_images=True,
                            include_links=True,
                            output_format="html"
                        )
                        
                        # Prepare result
                        result = {
                            "success": True if content else False,
                            "title": metadata.title if metadata and metadata.title else "",
                            "author": metadata.author if metadata and metadata.author else "",
                            "date": metadata.date if metadata and metadata.date else "",
                            "content": content if content else "",
                            "url": url,
                            "hostname": metadata.hostname if metadata and metadata.hostname else ""
                        }
                        
                        return result
                    except Exception as e:
                        return {
                            "success": False,
                            "error": str(e)
                        }

                if __name__ == "__main__":
                    if len(sys.argv) < 2:
                        print(json.dumps({
                            "success": False,
                            "error": "No URL provided"
                        }))
                        sys.exit(1)
                    
                    url = sys.argv[1]
                    result = extract_content(url)
                    print(json.dumps(result))
            """.trimIndent()
            
            FileWriter(file).use { it.write(scriptContent) }
            file.setExecutable(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Python script: ${e.message}", e)
        }
    }
    
    /**
     * Parse the output from the Python script
     */
    private fun parseOutput(output: String, originalUrl: String): ExtractionResult {
        try {
            // Try to parse JSON output
            val lines = output.trim().lines()
            val jsonLine = lines.last { it.trim().startsWith("{") && it.trim().endsWith("}") }
            
            // Simple JSON parsing to extract values
            val resultMap = mutableMapOf<String, String>()
            
            // Check if extraction was successful
            val successPattern = """"success":\s*(\w+)""".toRegex()
            val successMatch = successPattern.find(jsonLine)
            val success = successMatch?.groupValues?.get(1)?.lowercase() == "true"
            
            if (!success) {
                return createEmptyResult(originalUrl)
            }
            
            // Extract title
            val titlePattern = """"title":\s*"(.*?)"""".toRegex()
            val titleMatch = titlePattern.find(jsonLine)
            val title = titleMatch?.groupValues?.get(1)?.replace("\\\"", "\"") ?: ""
            
            // Extract content 
            val contentPattern = """"content":\s*"(.*?)"""".toRegex(RegexOption.DOT_MATCHES_ALL)
            val contentMatch = contentPattern.find(jsonLine)
            val content = contentMatch?.groupValues?.get(1)?.replace("\\\"", "\"")?.replace("\\n", "\n") ?: ""
            
            // Extract author
            val authorPattern = """"author":\s*"(.*?)"""".toRegex()
            val authorMatch = authorPattern.find(jsonLine)
            val author = authorMatch?.groupValues?.get(1)?.replace("\\\"", "\"") ?: ""
            
            // Extract date
            val datePattern = """"date":\s*"(.*?)"""".toRegex()
            val dateMatch = datePattern.find(jsonLine)
            val date = dateMatch?.groupValues?.get(1)?.replace("\\\"", "\"") ?: ""
            
            // Extract hostname
            val hostnamePattern = """"hostname":\s*"(.*?)"""".toRegex()
            val hostnameMatch = hostnamePattern.find(jsonLine)
            val hostname = hostnameMatch?.groupValues?.get(1)?.replace("\\\"", "\"") ?: ""
            
            return ExtractionResult(
                title = title.ifEmpty { "Untitled Article" },
                content = content,
                author = author,
                date = date,
                url = originalUrl,
                hostname = hostname,
                success = content.isNotEmpty()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Python output: ${e.message}", e)
            return createEmptyResult(originalUrl)
        }
    }
    
    /**
     * Create an empty result in case of failure
     */
    private fun createEmptyResult(url: String): ExtractionResult {
        return ExtractionResult(
            title = "Failed to extract content",
            content = "",
            author = "",
            date = "",
            url = url,
            hostname = "",
            success = false
        )
    }
}