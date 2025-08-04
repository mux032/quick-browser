package com.quick.browser.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.WebView
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

/**
 * Handles authentication URLs by launching them in Chrome Custom Tabs
 * instead of WebView to comply with Google's security policies.
 */
class AuthenticationHandler {
    companion object {
        private const val TAG = "AuthenticationHandler"
        
        // List of domains that require secure authentication handling
        private val AUTH_DOMAINS = listOf(
            "accounts.google.com",
            "accounts.google.",  // Catch all regional Google account domains
            "myaccount.google.com",
            "google.com/accounts",
            "signin.aws.amazon.com",
            "login.live.com",
            "login.yahoo.com",
            "login.microsoftonline.com",
            "api.login.yahoo.com",
            "login.facebook.com",
            "facebook.com/login",
            "login.microsoft.com",
            "twitter.com/oauth",
            "api.twitter.com/oauth",
            "github.com/login",
            "id.apple.com",
            "appleid.apple.com",
            "auth.amazon.com",
            "linkedin.com/oauth",
            "instagram.com/oauth",
            "pinterest.com/oauth",
            "oauth.reddit.com",
            "discord.com/oauth2",
            "slack.com/oauth"
        )

        // List of URL patterns that indicate OAuth or authentication flows
        private val AUTH_PATTERNS = listOf(
            "/oauth",
            "/oauth2",
            "/login",
            "/signin",
            "/auth",
            "/authenticate",
            "/authorize",
            "/accounts/",
            "/ServiceLogin",
            "/signin/oauth",
            "/o/oauth2/",
            "/gsi/",
            "/v3/signin"
        )
        
        // List of query parameters that indicate authentication
        private val AUTH_QUERY_PARAMS = listOf(
            "oauth",
            "login",
            "signin",
            "auth",
            "authenticate",
            "token",
            "code",
            "client_id",
            "redirect_uri",
            "response_type",
            "scope"
        )

        /**
         * Checks if the given URL is an authentication URL that should be handled securely
         * @param url The URL to check
         * @return True if the URL should be handled with Custom Tabs, false otherwise
         */
        fun isAuthenticationUrl(url: String): Boolean {
            try {
                val uri = url.toUri()
                val host = uri.host?.lowercase() ?: return false
                val path = uri.path?.lowercase() ?: ""
                val query = uri.query?.lowercase() ?: ""
                
                // Always handle Google authentication URLs with Custom Tabs
                if (host.contains("google") && 
                    (host.contains("account") || path.contains("account") || 
                     path.contains("auth") || path.contains("login") || 
                     path.contains("signin") || query.contains("auth"))) {
                    Log.d(TAG, "Google authentication URL detected: $url")
                    return true
                }
                
                // Check if the domain is in our list of authentication domains
                for (authDomain in AUTH_DOMAINS) {
                    if (host.contains(authDomain)) {
                        Log.d(TAG, "Authentication domain detected: $host in URL: $url")
                        return true
                    }
                }
                
                // Check if the URL path contains authentication patterns
                for (pattern in AUTH_PATTERNS) {
                    if (path.contains(pattern)) {
                        Log.d(TAG, "Authentication path pattern detected: $pattern in URL: $url")
                        return true
                    }
                }
                
                // Check query parameters for authentication indicators
                if (query.isNotEmpty()) {
                    for (param in AUTH_QUERY_PARAMS) {
                        if (query.contains(param)) {
                            Log.d(TAG, "Authentication query parameter detected: $param in URL: $url")
                            return true
                        }
                    }
                }
                
                return false
            } catch (e: Exception) {
                Log.e(TAG, "Error checking if URL is authentication URL", e)
                return false
            }
        }

        // Define a custom scheme for redirecting back to our app
        private const val REDIRECT_SCHEME = "quick_browser"
        private const val REDIRECT_HOST = "auth-callback"
        
        /**
         * Opens the given URL in Chrome Custom Tabs
         * @param context The context to use for launching the Custom Tab
         * @param url The URL to open
         * @param originalBubbleId The ID of the bubble that initiated the authentication (can be null)
         * @return True if the URL was opened successfully, false otherwise
         */
        fun openInCustomTab(context: Context, url: String, originalBubbleId: String? = null): Boolean {
            return try {
                Log.d(TAG, "Opening URL in Custom Tab: $url")
                
                // Create a redirect URI for returning to our app
                val redirectUri = Uri.Builder()
                    .scheme(REDIRECT_SCHEME)
                    .authority(REDIRECT_HOST)
                    .appendQueryParameter("original_url", url)
                    .appendQueryParameter("bubble_id", originalBubbleId ?: "")
                    .build()
                
                Log.d(TAG, "Redirect URI: $redirectUri")
                
                // First try with Custom Tabs
                val customTabsBuilder = CustomTabsIntent.Builder()
                    .setShowTitle(true)
                    .setUrlBarHidingEnabled(false)
                    .setShareState(CustomTabsIntent.SHARE_STATE_ON)
                
                // Add color customization
                val colorScheme = CustomTabColorSchemeParams.Builder()
                    .build()
                customTabsBuilder.setDefaultColorSchemeParams(colorScheme)
                
                val customTabsIntent = customTabsBuilder.build()
                
                // Add flags to ensure it opens in a new task
                customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                
                // Launch the URL in Custom Tabs
                customTabsIntent.launchUrl(context, url.toUri())
                Log.d(TAG, "Successfully launched URL in Custom Tab: $url")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error opening Custom Tab, falling back to system browser", e)
                
                // Fallback to system browser if Custom Tabs fails
                try {
                    val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    Log.d(TAG, "Successfully launched URL in system browser: $url")
                    true
                } catch (e2: Exception) {
                    Log.e(TAG, "Error opening URL in system browser", e2)
                    false
                }
            }
        }

        /**
         * Handles the authentication return from Chrome Custom Tabs.
         * @param uri The callback URI received after authentication.
         * @return True if the authentication was handled successfully, false otherwise.
         */
        fun handleAuthenticationReturn(uri: Uri): Boolean {
            return try {
                Log.d(TAG, "Processing authentication return URI: $uri")

                // Validate the URI (e.g., check scheme, host, and required parameters)
                if (uri.scheme != REDIRECT_SCHEME || uri.host != REDIRECT_HOST) {
                    Log.e(TAG, "Invalid authentication callback URI")
                    return false
                }

                // Extract parameters (e.g., token or state)
                val token = uri.getQueryParameter("token")
                val state = uri.getQueryParameter("state")

                if (token.isNullOrEmpty()) {
                    Log.e(TAG, "Missing token in authentication callback")
                    return false
                }

                // Notify the WebView to reload or update its state
                val webView = getWebViewForState(state)
                if (webView != null) {
                    val redirectUrl = uri.getQueryParameter("redirect_url") ?: "https://example.com"
                    webView.post {
                        webView.loadUrl(redirectUrl)
                    }
                    Log.d(TAG, "WebView updated with redirect URL: $redirectUrl")
                } else {
                    Log.w(TAG, "No WebView found for state: $state")
                }

                true
            } catch (e: Exception) {
                Log.e(TAG, "Error handling authentication return: ${e.message}", e)
                false
            }
        }

        private val webViewStateMap = mutableMapOf<String, WebView>()

        /**
         * Associates a WebView with a specific state.
         * This should be called when initiating an authentication flow.
         * @param state The state parameter used to track the WebView.
         * @param webView The WebView instance to associate with the state.
         */
        fun associateWebViewWithState(state: String, webView: WebView) {
            webViewStateMap[state] = webView
            Log.d(TAG, "Associated WebView with state: $state")
        }

        /**
         * Retrieves the WebView associated with the given state.
         * @param state The state parameter from the authentication callback.
         * @return The WebView instance or null if not found.
         */
        private fun getWebViewForState(state: String?): WebView? {
            if (state == null) {
                Log.w(TAG, "State is null, cannot retrieve WebView")
                return null
            }

            val webView = webViewStateMap[state]
            if (webView != null) {
                Log.d(TAG, "Retrieved WebView for state: $state")
            } else {
                Log.w(TAG, "No WebView found for state: $state")
            }
            return webView
        }

        /**
         * Removes the WebView associated with a specific state.
         * This should be called after the authentication flow is complete.
         * @param state The state parameter used to track the WebView.
         */
        fun removeWebViewForState(state: String) {
            webViewStateMap.remove(state)
            Log.d(TAG, "Removed WebView association for state: $state")
        }
    }
}