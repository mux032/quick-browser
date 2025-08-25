package com.quick.browser.service

import android.content.Context
import com.quick.browser.utils.ErrorHandler
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URI

/**
 * Utility for extracting readable content from web pages
 */
class ReadabilityService(private val context: Context) {
    
    data class ReadableContent(
        val title: String,
        val content: String,
        val byline: String? = null,
        val excerpt: String? = null,
        val siteName: String? = null,
        val publishDate: String? = null
    )
    
    companion object {
        private const val TAG = "ReadabilityExtractor"
        private const val TIMEOUT_MS = 10000
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/86.0.4240.110 Mobile Safari/537.36"
    }
    
    /**
     * Extract readable content from a URL
     */
    suspend fun extractFromUrl(url: String): ReadableContent? =
        ErrorHandler.handleExceptions(
            tag = TAG,
            errorMessage = "Failed to extract content from URL: $url"
        ) {
            val doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.5")
                .header("Accept-Encoding", "gzip, deflate, br")
                .header("Connection", "keep-alive")
                .header("Upgrade-Insecure-Requests", "1")
                .header("Sec-Fetch-Dest", "document")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-Site", "none")
                .header("Cache-Control", "max-age=0")
                .timeout(TIMEOUT_MS)
                .get()
            extractFromDocument(doc, url)
        }
    
    /**
     * Extract readable content from HTML string
     */
    suspend fun extractFromHtml(html: String, baseUrl: String): ReadableContent? =
        ErrorHandler.handleExceptions(
            tag = TAG,
            errorMessage = "Failed to extract content from HTML"
        ) {
            val doc = Jsoup.parse(html, baseUrl)
            extractFromDocument(doc, baseUrl)
        }
    
    /**
     * Extract content from a Jsoup Document
     */
    private fun extractFromDocument(doc: Document, url: String): ReadableContent {
        // Get the title of the page
        val title = doc.title() ?: "Untitled"
        
        // Try to find the main content
        val mainContent = findMainContent(doc)
        
        // Get the byline if available
        val byline = findByline(doc)
        
        // Get an excerpt of the content
        val excerpt = createExcerpt(mainContent)
        
        // Get the site name
        val siteName = findSiteName(doc, url)
        
        // Get publish date if available
        val publishDate = findPublishDate(doc)
        
        return ReadableContent(
            title = title,
            content = mainContent,
            byline = byline,
            excerpt = excerpt,
            siteName = siteName,
            publishDate = publishDate
        )
    }
    
    /**
     * Find the main content of the page
     */
    private fun findMainContent(doc: Document): String {
        // Remove unnecessary elements
        doc.select("script, style, iframe, ins, nav, footer, header, aside, .nav, .menu, .advertisement, .ads, .ad, .banner").remove()
        
        // Try to find the main article element
        val articleElements = listOf(
            doc.select("article").first(),
            doc.select(".post-content").first(),
            doc.select(".entry-content").first(),
            doc.select(".content").first(),
            doc.select(".post").first(),
            doc.select("#content").first(),
            doc.select("main").first(),
            doc.select(".main").first()
        ).filterNotNull()
        
        if (articleElements.isNotEmpty()) {
            return cleanAndFormatContent(articleElements.first())
        }
        
        // If no article element found, try to find paragraphs
        val paragraphs = doc.select("p")
        if (paragraphs.isNotEmpty()) {
            // Find element containing the most paragraphs
            val parents = HashMap<Element, Int>()
            
            paragraphs.forEach { p ->
                val parent = p.parent()
                if (parent != null) {
                    parents[parent] = (parents[parent] ?: 0) + 1
                }
            }
            
            val bestParent = parents.entries.maxByOrNull { it.value }?.key
            
            return if (bestParent != null) {
                cleanAndFormatContent(bestParent)
            } else {
                val sbContent = StringBuilder()
                paragraphs.forEach { p ->
                    if (p.text().length > 50) {  // Only include substantial paragraphs
                        sbContent.append("<p>").append(p.text()).append("</p>")
                    }
                }
                sbContent.toString()
            }
        }
        
        // If all else fails, just return the body text
        return doc.body().text()
    }
    
    /**
     * Clean and format content for readability
     */
    private fun cleanAndFormatContent(element: Element): String {
        // Make a copy to avoid modifying the original
        val clone = element.clone()
        
        // Remove unwanted elements
        clone.select("script, style, iframe, ins, nav, footer, header, aside, .nav, .menu, .advertisement, .ads, .ad, .banner").remove()
        
        // Remove empty paragraphs
        clone.select("p").forEach { p ->
            if (p.text().isBlank()) {
                p.remove()
            }
        }
        
        // If it has good HTML structure, keep it, otherwise format as paragraphs
        return if (clone.select("p, h1, h2, h3, h4, h5, h6, blockquote, ul, ol").isNotEmpty()) {
            clone.html()
        } else {
            val text = clone.text()
            val paragraphs = text.split("(?<=\\.)\\s+".toRegex())
            val sbContent = StringBuilder()
            paragraphs.forEach { p ->
                if (p.isNotEmpty()) {
                    sbContent.append("<p>").append(p).append("</p>")
                }
            }
            sbContent.toString()
        }
    }
    
    /**
     * Find the author/byline in the article
     */
    private fun findByline(doc: Document): String? {
        val possibleBylineElements = listOf(
            doc.select(".byline").first(),
            doc.select(".author").first(),
            doc.select("[rel=author]").first(),
            doc.select("[itemprop=author]").first(),
            doc.select(".entry-author").first()
        ).filterNotNull()
        
        return possibleBylineElements.firstOrNull()?.text()
    }
    
    /**
     * Create a brief excerpt from the content
     */
    private fun createExcerpt(content: String): String? {
        // Parse the HTML content
        val doc = Jsoup.parse(content)
        
        // Get the text and limit to first 200 characters
        val text = doc.text()
        return if (text.length > 200) {
            text.substring(0, 200) + "..."
        } else {
            text
        }
    }
    
    /**
     * Find the site name
     */
    private fun findSiteName(doc: Document, url: String): String? {
        // Try to find site name from meta tags
        val metaSiteName = doc.select("meta[property=og:site_name]").first()?.attr("content")
        if (!metaSiteName.isNullOrBlank()) {
            return metaSiteName
        }
        
        // Try to extract from URL
        return try {
            val uri = URI(url)
            val host = uri.host
            host.substringAfterLast('.', host)
                .substringAfterLast('.', host)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Find the publish date
     */
    private fun findPublishDate(doc: Document): String? {
        val possibleDateElements = listOf(
            doc.select("meta[property=article:published_time]").first()?.attr("content"),
            doc.select("meta[name=publish-date]").first()?.attr("content"),
            doc.select("time").first()?.attr("datetime"),
            doc.select(".date").first()?.text(),
            doc.select(".published").first()?.text(),
            doc.select(".timestamp").first()?.text()
        ).filterNotNull().firstOrNull { it.isNotBlank() }
        
        return possibleDateElements
    }
}
