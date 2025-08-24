package com.quick.browser.manager

import android.content.Context
import android.webkit.WebResourceResponse
import com.quick.browser.util.ErrorHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * AdBlocker utility to block ads and trackers
 */
class AdBlocker(private val context: Context) {

    private val adServerHosts = HashSet<String>()
    private val cachedResults = ConcurrentHashMap<String, Boolean>()
    private val whitelistedDomains = HashSet<String>()
    private val blacklistedDomains = HashSet<String>()
    private val encryptedPrefs = EncryptedPreferences.getInstance(context, "adblocker_encrypted_settings")

    // Class tag for logging
    private val TAG = "AdBlocker"

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
            ErrorHandler.logError(TAG, "Error checking if URL should be blocked: $url", e)
        }.getOrNull()
    }

    /**
     * Add a host to the blocked list
     */
    fun addBlockedHost(host: String) {
        adServerHosts.add(host)
        saveRules()
    }

    /**
     * Remove a host from the blocked list
     */
    fun removeBlockedHost(host: String) {
        adServerHosts.remove(host)
        cachedResults.remove(host)
        saveRules()
    }

    /**
     * Add a domain to the whitelist (never blocked)
     */
    fun addToWhitelist(domain: String) {
        whitelistedDomains.add(domain)
        saveWhitelist()
    }

    /**
     * Remove a domain from the whitelist
     */
    fun removeFromWhitelist(domain: String) {
        whitelistedDomains.remove(domain)
        saveWhitelist()
    }

    /**
     * Add a domain to the blacklist (always blocked)
     */
    fun addToBlacklist(domain: String) {
        blacklistedDomains.add(domain)
        saveBlacklist()
    }

    /**
     * Remove a domain from the blacklist
     */
    fun removeFromBlacklist(domain: String) {
        blacklistedDomains.remove(domain)
        saveBlacklist()
    }

    /**
     * Check if a domain is whitelisted
     */
    fun isWhitelisted(domain: String): Boolean {
        return whitelistedDomains.contains(domain)
    }

    /**
     * Check if a domain is blacklisted
     */
    fun isBlacklisted(domain: String): Boolean {
        return blacklistedDomains.contains(domain)
    }

    /**
     * Get list of blocked hosts
     */
    fun getBlockedHosts(): Set<String> {
        return adServerHosts.toSet()
    }

    /**
     * Get whitelisted domains
     */
    fun getWhitelist(): Set<String> {
        return whitelistedDomains.toSet()
    }

    /**
     * Get blacklisted domains
     */
    fun getBlacklist(): Set<String> {
        return blacklistedDomains.toSet()
    }

    /**
     * Update ad blocking rules from a URL
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
            ErrorHandler.logError(TAG, "Failed to update ad blocking rules from $url", e)
            false
        }

    /**
     * Parse hosts from a text file
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
     */
    private fun extractDomain(url: String): String {
        return runCatching {
            // Skip data URLs and other non-standard protocols that can't be parsed
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return@runCatching url
            }
            
            val uri = URL(url)
            val host = uri.host
            if (host.startsWith("www.")) host.substring(4) else host
        }.getOrDefault(url)
    }

    /**
     * Load ad blocking rules from preferences
     */
    private fun loadRules() {
        val serializedRules = encryptedPrefs.getStringSet("rules", null) ?: getDefaultRules()
        adServerHosts.clear()
        adServerHosts.addAll(serializedRules)
    }

    /**
     * Save ad blocking rules to preferences
     */
    private fun saveRules() {
        encryptedPrefs.putStringSet("rules", adServerHosts)
    }

    /**
     * Load whitelist from preferences
     */
    private fun loadWhitelist() {
        val whitelist = encryptedPrefs.getStringSet("whitelist_domains", null) ?: emptySet()
        whitelistedDomains.clear()
        whitelistedDomains.addAll(whitelist)
    }

    /**
     * Save whitelist to preferences
     */
    private fun saveWhitelist() {
        encryptedPrefs.putStringSet("whitelist_domains", whitelistedDomains)
    }

    /**
     * Load blacklist from preferences
     */
    private fun loadBlacklist() {
        val blacklist = encryptedPrefs.getStringSet("blacklist_domains", null) ?: emptySet()
        blacklistedDomains.clear()
        blacklistedDomains.addAll(blacklist)
    }

    /**
     * Save blacklist to preferences
     */
    private fun saveBlacklist() {
        encryptedPrefs.putStringSet("blacklist_domains", blacklistedDomains)
    }

    /**
     * Get default ad blocking rules
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