package com.qb.browser.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URL

/**
 * Utility for extracting readable content from web pages
 */
class ContentExtractor(private val context: Context) {
    
    // Data class to hold extracted content
    data class ReadableContent(
        val title: String,
        val content: String,
        val byline: String = "",
        val siteName: String = "",
        val imageUrl: String = "",
        val date: String = ""
    )
    
    private val pythonExtractor = PythonExtractor(context)
    
    companion object {
        private const val TAG = "ContentExtractor"
        
        // Content quality indicators
        private const val MIN_CONTENT_LENGTH = 140 // Minimum length for a paragraph to be considered content
        private const val MIN_SCORE_THRESHOLD = 20 // Minimum score for a node to be considered part of the main content
        
        // Tags to remove from the final content
        private val UNWANTED_TAGS = setOf(
            "script", "style", "noscript", "iframe", "form", "object", "embed", "header", "footer", 
            "nav", "aside", "button", "[class*=comment]", "[id*=comment]", "[class*=share]", "[id*=share]",
            "[class*=social]", "[id*=social]", "[class*=promo]", "[id*=promo]", "[class*=ad-]",
            "[id*=ad-]", "[class*=advertisement]", "[id*=advertisement]", "[class*=banner]", "[id*=banner]"
        )
    }
    
    /**
     * Extract readable content from a URL
     */
    suspend fun extractReadableContent(url: String): ReadableContent = 
        withErrorHandlingAndFallback(
            tag = TAG,
            errorMessage = "Error extracting content from $url",
            fallback = ReadableContent(
                title = "Could not extract content",
                content = "Failed to extract content from the page. Please try again or view the original page.",
                byline = "",
                siteName = ""
            )
        ) {
            // First try with Python's trafilatura for best results
            val pythonResult = pythonExtractor.extractContent(url)
            
            if (pythonResult.success && pythonResult.content.isNotEmpty()) {
                ReadableContent(
                    title = pythonResult.title,
                    content = pythonResult.content,
                    byline = pythonResult.author,
                    siteName = pythonResult.hostname,
                    date = pythonResult.date
                )
            } else {
                // Fall back to JSoup extraction
                Log.d(TAG, "Python extraction failed, falling back to JSoup")
                val document = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(10000)
                    .get()
                
                extractWithJsoup(document, url)
            }
        }
    
    /**
     * Extract content using JSoup as a fallback method
     */
    private fun extractWithJsoup(document: Document, url: String): ReadableContent {
        // Extract title
        val title = document.title() ?: document.select("h1").firstOrNull()?.text() ?: "Untitled"
        
        // Try to extract author/byline
        val byline = findByline(document)
        
        // Try to extract date
        val date = findPublicationDate(document)
        
        // Clean the document
        cleanDocument(document)
        
        // Find content
        val contentElement = findMainContentElement(document)
        val content = if (contentElement != null) {
            cleanupContent(contentElement)
            contentElement.html()
        } else {
            // Fallback to basic article extraction
            document.select("p")
                .filter { element -> element.text().length > MIN_CONTENT_LENGTH }
                .joinToString("\n") { element -> "<p>${element.html()}</p>" }
        }
        
        // Extract hostname for site name
        val hostname = try {
            URL(url).host
        } catch (e: Exception) {
            ""
        }
        
        // Extract main image if available
        val mainImage = findMainImage(document)
        
        return ReadableContent(
            title = title,
            content = content,
            byline = byline,
            siteName = hostname,
            imageUrl = mainImage,
            date = date
        )
    }
    
    /**
     * Find the byline/author information
     */
    private fun findByline(document: Document): String {
        // Common selectors for author information
        val authorSelectors = listOf(
            "[rel=author]",
            ".byline",
            ".author",
            "[itemprop=author]",
            ".article-meta a",
            ".article-byline",
            "[class*=author]",
            "[class*=byline]",
            "meta[name=author]",
            "meta[property=article:author]"
        )
        
        for (selector in authorSelectors) {
            try {
                if (selector.startsWith("meta")) {
                    // Handle meta tags
                    val element = document.select(selector).firstOrNull()
                    if (element != null) {
                        val content = element.attr("content")
                        if (content.isNotEmpty()) {
                            return content
                        }
                    }
                } else {
                    // Handle regular elements
                    val element = document.select(selector).firstOrNull()
                    if (element != null && element.text().isNotEmpty()) {
                        return element.text()
                    }
                }
            } catch (e: Exception) {
                // Ignore selector errors
            }
        }
        
        return ""
    }
    
    /**
     * Find the publication date
     */
    private fun findPublicationDate(document: Document): String {
        // Common selectors for publication date
        val dateSelectors = listOf(
            "[itemprop=datePublished]",
            "meta[property=article:published_time]",
            "meta[name=date]",
            "meta[name=pubdate]",
            ".published",
            ".date",
            "[class*=date]",
            "[class*=time]",
            "time"
        )
        
        for (selector in dateSelectors) {
            try {
                if (selector.startsWith("meta")) {
                    // Handle meta tags
                    val element = document.select(selector).firstOrNull()
                    if (element != null) {
                        val content = element.attr("content")
                        if (content.isNotEmpty()) {
                            return content
                        }
                    }
                } else {
                    // Handle regular elements
                    val element = document.select(selector).firstOrNull()
                    if (element != null) {
                        // Check for datetime attribute first
                        val datetime = element.attr("datetime")
                        if (datetime.isNotEmpty()) {
                            return datetime
                        }
                        
                        // Fall back to text content
                        if (element.text().isNotEmpty()) {
                            return element.text()
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore selector errors
            }
        }
        
        return ""
    }
    
    /**
     * Find the main image
     */
    private fun findMainImage(document: Document): String {
        // Look for Open Graph image
        val ogImage = document.select("meta[property=og:image]").firstOrNull()?.attr("content")
        if (!ogImage.isNullOrEmpty()) {
            return ogImage
        }
        
        // Look for Twitter image
        val twitterImage = document.select("meta[name=twitter:image]").firstOrNull()?.attr("content")
        if (!twitterImage.isNullOrEmpty()) {
            return twitterImage
        }
        
        // Look for schema.org image
        val schemaImage = document.select("[itemprop=image]").firstOrNull()
        if (schemaImage != null) {
            val src = schemaImage.attr("src")
            val content = schemaImage.attr("content")
            return if (src.isNotEmpty()) src else content
        }
        
        // Look for the largest image in the article content
        val contentElement = findMainContentElement(document)
        if (contentElement != null) {
            val images = contentElement.select("img")
            if (images.isNotEmpty()) {
                // Try to find a reasonably sized image
                val largeImage = images.firstOrNull { img -> 
                    val width = img.attr("width").toIntOrNull() ?: 0
                    val height = img.attr("height").toIntOrNull() ?: 0
                    width >= 300 && height >= 200
                }
                
                if (largeImage != null) {
                    return largeImage.absUrl("src")
                }
                
                // Fall back to the first image, if one exists
                return images.firstOrNull()?.absUrl("src") ?: ""
            }
        }
        
        return ""
    }
    
    /**
     * Clean the document by removing unwanted elements
     */
    private fun cleanDocument(document: Document) {
        // Remove unwanted elements
        UNWANTED_TAGS.forEach { selector ->
            try {
                document.select(selector).remove()
            } catch (e: Exception) {
                // Ignore selector errors
            }
        }
        
        // Remove non-visible elements
        document.select("[style~=display:\\s*none]").remove()
        document.select("[hidden]").remove()
        
        // Remove empty paragraphs
        document.select("p").forEach { p ->
            if (p.text().trim().isEmpty() && p.select("img").isEmpty()) {
                p.remove()
            }
        }
    }
    
    /**
     * Find the main content element using a scoring algorithm
     */
    private fun findMainContentElement(document: Document): Element? {
        val contentScores = mutableMapOf<Element, Int>()
        
        // Score potential content containers
        val potentialElements = document.select("div, article, section, main")
        
        for (element in potentialElements) {
            var score = 0
            
            // Score based on ID/class names
            val classAndId = (element.className() + " " + element.id()).lowercase()
            
            // Positive indicators
            val positiveTerms = listOf("article", "content", "entry", "main", "text", "story", "post", "body")
            for (term in positiveTerms) {
                if (classAndId.contains(term)) {
                    score += 25
                    break
                }
            }
            
            // Negative indicators
            val negativeTerms = listOf("sidebar", "widget", "menu", "nav", "comment", "footer", "header", "ad", "banner")
            for (term in negativeTerms) {
                if (classAndId.contains(term)) {
                    score -= 25
                    break
                }
            }
            
            // Score based on contained elements
            val paragraphs = element.select("p")
            score += paragraphs.size * 3
            
            // Give extra points for longer paragraphs
            val longParagraphs = paragraphs.count { paragraph -> paragraph.text().length > MIN_CONTENT_LENGTH }
            score += longParagraphs * 5
            
            // Score for headings
            score += element.select("h1, h2, h3, h4, h5, h6").size * 2
            
            // Score for images
            score += element.select("img").size * 1
            
            // Score for links (but not too many)
            val links = element.select("a").size
            score -= if (links > paragraphs.size * 2) 10 else 0
            
            // Store score if it meets minimum threshold
            if (score >= MIN_SCORE_THRESHOLD) {
                contentScores[element] = score
            }
        }
        
        // Find highest scoring element
        var bestElement: Element? = null
        var bestScore = MIN_SCORE_THRESHOLD
        
        for ((element, score) in contentScores) {
            if (score > bestScore) {
                bestElement = element
                bestScore = score
            }
        }
        
        return bestElement
    }
    
    /**
     * Clean up the content element for better readability
     */
    private fun cleanupContent(element: Element) {
        // Convert relative URLs to absolute
        element.select("a[href]").forEach { link ->
            try {
                val absUrl = link.absUrl("href")
                if (absUrl.isNotEmpty()) {
                    link.attr("href", absUrl)
                }
            } catch (e: Exception) {
                // Ignore URL conversion errors
            }
        }
        
        element.select("img[src]").forEach { img ->
            try {
                val absUrl = img.absUrl("src")
                if (absUrl.isNotEmpty()) {
                    img.attr("src", absUrl)
                    
                    // Add alt text if missing
                    if (img.attr("alt").isEmpty()) {
                        img.attr("alt", "Image")
                    }
                    
                    // Add loading=lazy for performance
                    img.attr("loading", "lazy")
                }
            } catch (e: Exception) {
                // Ignore URL conversion errors
            }
        }
        
        // Remove empty links
        element.select("a").forEach { a ->
            if (a.text().trim().isEmpty() && a.select("img").isEmpty()) {
                a.unwrap() // Preserve content but remove the link
            }
        }
        
        // Clean attributes to simplify the HTML
        element.select("*").forEach { el ->
            // Keep only essential attributes
            val attrsToKeep = listOf("href", "src", "alt", "title", "loading")
            val attrsToRemove = mutableListOf<String>()
            
            for (attr in el.attributes()) {
                if (!attrsToKeep.contains(attr.key)) {
                    attrsToRemove.add(attr.key)
                }
            }
            
            attrsToRemove.forEach { attr ->
                el.removeAttr(attr)
            }
        }
    }
}