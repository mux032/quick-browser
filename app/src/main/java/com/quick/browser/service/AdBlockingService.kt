package com.quick.browser.service

import android.content.Context
import android.webkit.WebResourceResponse
import com.quick.browser.domain.service.EncryptedPreferencesService
import com.quick.browser.utils.Logger
import com.quick.browser.utils.LoggingTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * AdBlocker utility to block ads and trackers
 *
 * This service provides functionality to block advertisements and tracking
 * requests based on predefined rules and user-configurable lists. It maintains
 * in-memory caches for performance and supports domain whitelisting/blacklisting.
 *
 * @param context The application context
 * @param encryptedPrefs The encrypted preferences service for storing user configurations
 */
class AdBlockingService(private val context: Context, private val encryptedPrefs: EncryptedPreferencesService) : LoggingTag {

    private val adServerHosts = HashSet<String>()
    private val cachedResults = ConcurrentHashMap<String, Boolean>()
    private val whitelistedDomains = HashSet<String>()
    private val blacklistedDomains = HashSet<String>()

    init {
        // Load ad blocking rules from preferences or default list
        loadRules()
        loadWhitelist()
        loadBlacklist()
    }

    companion object {
        private const val PREFS_AD_SERVERS = "ad_servers"
        private const val PREFS_WHITELIST = "whitelist_domains"
        private const val PREFS_BLACKLIST = "blacklist_domains"

        // Empty response to return for blocked requests
        private val EMPTY_RESPONSE = WebResourceResponse(
            "text/plain",
            "UTF-8",
            ByteArrayInputStream("".toByteArray())
        )
    }

    /**
     * Check if a request should be blocked
     *
     * @param url The URL of the request to check
     * @return A WebResourceResponse if the request should be blocked, null otherwise
     */
    fun shouldBlockRequest(url: String): WebResourceResponse? {
        return runCatching {
            // Quick check for common non-ad resources to improve performance
            if (url.endsWith(".css") || url.endsWith(".png") ||
                url.endsWith(".jpg") || url.endsWith(".jpeg") ||
                url.endsWith(".gif") || url.endsWith(".svg") ||
                url.endsWith(".woff") || url.endsWith(".woff2") ||
                url.endsWith(".ttf")
            ) {
                return@runCatching null
            }

            // Skip data URLs and other non-standard protocols that can't be parsed
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return@runCatching null
            }

            val hostname = URL(url).host ?: return@runCatching null

            // Check domain whitelist/blacklist first
            val domain = extractDomain(url)
            if (isWhitelisted(domain)) return@runCatching null
            if (isBlacklisted(domain)) return@runCatching EMPTY_RESPONSE

            // Use cached result if available
            cachedResults[hostname]?.let {
                return@runCatching if (it) EMPTY_RESPONSE else null
            }

            // Check for common ad patterns in URL
            if (url.contains("/ads/") || url.contains("/ad/") || url.contains("/analytics/") ||
                url.contains("/tracker/") || url.contains("/pixel/") || url.contains("/banner/")
            ) {
                cachedResults[hostname] = true
                return@runCatching EMPTY_RESPONSE
            }

            val shouldBlock = adServerHosts.contains(hostname)
            cachedResults[hostname] = shouldBlock

            if (shouldBlock) EMPTY_RESPONSE else null
        }.onFailure { e ->
            Logger.e(tag, "Error checking if URL should be blocked: $url", e)
        }.getOrNull()
    }

    /**
     * Add a host to the blocked list
     *
     * @param host The host to add to the blocked list
     */
    fun addBlockedHost(host: String) {
        adServerHosts.add(host)
        saveRules()
    }

    /**
     * Remove a host from the blocked list
     *
     * @param host The host to remove from the blocked list
     */
    fun removeBlockedHost(host: String) {
        adServerHosts.remove(host)
        cachedResults.remove(host)
        saveRules()
    }

    /**
     * Add a domain to the whitelist (never blocked)
     *
     * @param domain The domain to add to the whitelist
     */
    fun addToWhitelist(domain: String) {
        whitelistedDomains.add(domain)
        saveWhitelist()
    }

    /**
     * Remove a domain from the whitelist
     *
     * @param domain The domain to remove from the whitelist
     */
    fun removeFromWhitelist(domain: String) {
        whitelistedDomains.remove(domain)
        saveWhitelist()
    }

    /**
     * Add a domain to the blacklist (always blocked)
     *
     * @param domain The domain to add to the blacklist
     */
    fun addToBlacklist(domain: String) {
        blacklistedDomains.add(domain)
        saveBlacklist()
    }

    /**
     * Remove a domain from the blacklist
     *
     * @param domain The domain to remove from the blacklist
     */
    fun removeFromBlacklist(domain: String) {
        blacklistedDomains.remove(domain)
        saveBlacklist()
    }

    /**
     * Check if a domain is whitelisted
     *
     * @param domain The domain to check
     * @return True if the domain is whitelisted, false otherwise
     */
    fun isWhitelisted(domain: String): Boolean {
        return whitelistedDomains.contains(domain)
    }

    /**
     * Check if a domain is blacklisted
     *
     * @param domain The domain to check
     * @return True if the domain is blacklisted, false otherwise
     */
    fun isBlacklisted(domain: String): Boolean {
        return blacklistedDomains.contains(domain)
    }

    /**
     * Get list of blocked hosts
     *
     * @return A set of blocked hosts
     */
    fun getBlockedHosts(): Set<String> {
        return adServerHosts.toSet()
    }

    /**
     * Get whitelisted domains
     *
     * @return A set of whitelisted domains
     */
    fun getWhitelist(): Set<String> {
        return whitelistedDomains.toSet()
    }

    /**
     * Get blacklisted domains
     *
     * @return A set of blacklisted domains
     */
    fun getBlacklist(): Set<String> {
        return blacklistedDomains.toSet()
    }

    /**
     * Update ad blocking rules from a URL
     *
     * @param url The URL to fetch ad blocking rules from
     * @return True if the rules were updated successfully, false otherwise
     */
    suspend fun updateRulesFromUrl(url: String): Boolean =
        try {
            withContext(Dispatchers.IO) {
                val connection = URL(url).openConnection()
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                val inputStream = connection.getInputStream()
                val rulesText = inputStream.bufferedReader().use { it.readText() }
                val hosts = parseHosts(rulesText)

                if (hosts.isNotEmpty()) {
                    adServerHosts.clear()
                    adServerHosts.addAll(hosts)
                    saveRules()
                    cachedResults.clear()
                    true
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            Logger.e(tag, "Failed to update ad blocking rules from $url", e)
            false
        }

    /**
     * Load ad blocking rules from preferences
     */
    private fun loadRules() {
        val serializedRules = encryptedPrefs.getStringSet(PREFS_AD_SERVERS, null) ?: getDefaultRules()
        adServerHosts.clear()
        adServerHosts.addAll(serializedRules)
    }

    /**
     * Save ad blocking rules to preferences
     */
    private fun saveRules() {
        encryptedPrefs.putStringSet(PREFS_AD_SERVERS, adServerHosts)
    }

    /**
     * Load whitelist from preferences
     */
    private fun loadWhitelist() {
        val whitelist = encryptedPrefs.getStringSet(PREFS_WHITELIST, null) ?: emptySet()
        whitelistedDomains.clear()
        whitelistedDomains.addAll(whitelist)
    }

    /**
     * Save whitelist to preferences
     */
    private fun saveWhitelist() {
        encryptedPrefs.putStringSet(PREFS_WHITELIST, whitelistedDomains)
    }

    /**
     * Load blacklist from preferences
     */
    private fun loadBlacklist() {
        val blacklist = encryptedPrefs.getStringSet(PREFS_BLACKLIST, null) ?: emptySet()
        blacklistedDomains.clear()
        blacklistedDomains.addAll(blacklist)
    }

    /**
     * Save blacklist to preferences
     */
    private fun saveBlacklist() {
        encryptedPrefs.putStringSet(PREFS_BLACKLIST, blacklistedDomains)
    }

    /**
     * Parse hosts from a text file
     *
     * @param rulesText The text content to parse
     * @return A set of host names
     */
    private fun parseHosts(rulesText: String): Set<String> {
        val hosts = HashSet<String>()
        rulesText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .forEach { line ->
                val parts = line.split(Regex("\\s+"))
                if (parts.size >= 2 && parts[0] == "127.0.0.1") {
                    hosts.add(parts[1])
                } else if (parts.size == 1 && parts[0].isNotBlank()) {
                    hosts.add(parts[0])
                }
            }
        return hosts
    }

    /**
     * Extract domain from URL
     *
     * @param url The URL to extract domain from
     * @return The domain name
     */
    private fun extractDomain(url: String): String {
        return runCatching {
            // Skip data URLs and other non-standard protocols that can't be parsed
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return@runCatching ""
            }
            
            val uri = URL(url)
            val host = uri.host ?: return@runCatching ""
            if (host.startsWith("www.")) host.substring(4) else host
        }.getOrDefault("")
    }

    /**
     * Get default ad blocking rules
     *
     * @return A set of default ad blocking rules
     */
    private fun getDefaultRules(): Set<String> {
        return setOf(
            "ad.doubleclick.net",
            "googleads.g.doubleclick.net",
            "pagead2.googlesyndication.com",
            "pagead.googlesyndication.com",
            "tpc.googlesyndication.com",
            "pubads.g.doubleclick.net",
            "securepubads.g.doubleclick.net",
            "www.googleadservices.com",
            "adservice.google.com",
            "adfarm.mediaplex.com",
            "ads.facebook.com",
            "an.facebook.com",
            "analytics.twitter.com",
            "ads.twitter.com",
            "ads.yahoo.com",
            "ads.yap.yahoo.com",
            "adserver.yahoo.com"
        )
    }
}