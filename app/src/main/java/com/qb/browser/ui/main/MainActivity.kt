package com.qb.browser.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.qb.browser.Constants
import com.qb.browser.R
import com.qb.browser.manager.AuthenticationHandler
import com.qb.browser.service.BubbleService
import com.qb.browser.ui.base.BaseActivity
import com.qb.browser.ui.settings.SettingsActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {
    companion object {
        private const val TAG = "MainActivity"
        // Removed NOTIFICATION_PERMISSION constant as it's no longer needed
    }

    private lateinit var addressBar: EditText
    private lateinit var goButton: ImageButton

    // Activity result launcher for history activity
    private val historyActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedUrl = result.data?.getStringExtra(com.qb.browser.ui.history.HistoryActivity.EXTRA_SELECTED_URL)
            if (selectedUrl != null) {
                handleUrlInput(selectedUrl)
            }
        }
    }

    // Removed permissionLauncher as notification permission is now optional

    /** Starts the BubbleService to display floating bubbles. */
    private fun startBubbleService() {
        val intent =
            Intent(this, BubbleService::class.java).apply {
                action = Constants.ACTION_TOGGLE_BUBBLES
            }
        try {
            startForegroundService(intent)
            // Removed moveTaskToBack(true) to keep the app in foreground
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start bubble service", e)
            Toast.makeText(this, "Failed to start bubble service", Toast.LENGTH_SHORT).show()
        }
    }

    /** Starts the BubbleService with a specific URL to open in the bubble browser. */
    private fun startBubbleServiceWithUrl(url: String) {
        if (!checkPermissionsAndStartBubble()) {
            return
        }

        Log.d(TAG, "startBubbleServiceWithUrl: Starting service with URL: $url")
        val intent =
            Intent(this, BubbleService::class.java).apply {
                action = Constants.ACTION_CREATE_BUBBLE
                putExtra(Constants.EXTRA_URL, url)
            }

        try {
            startForegroundService(intent)
            // Removed moveTaskToBack(true) to keep the app in foreground
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start bubble service", e)
            Toast.makeText(this, "Failed to start bubble service", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate called with intent: ${intent?.action}")

        // Always call super.onCreate() to avoid SuperNotCalledException
        // Theme will be applied automatically by BaseActivity
        super.onCreate(savedInstanceState)

        // Always set content view to ensure activity is properly initialized
        setContentView(R.layout.activity_main)

        // Check if this is a link sharing intent before setting up the rest of the UI
        if (isLinkSharingIntent(intent)) {
            Log.d(TAG, "Link sharing intent detected in onCreate, handling without full UI initialization")
            // Handle the intent without initializing the full UI
            handleLinkSharingIntent(intent)
            return
        }

        // Set up toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false) // Hide default title

        // settingsManager is already initialized in BaseActivity

        // Initialize views
        addressBar = findViewById(R.id.address_bar)
        goButton = findViewById(R.id.go_button)

        // Set up address bar
        setupAddressBar()

        // Handle main app intent (not link sharing)
        handleMainAppIntent()
    }

    private fun openHistoryActivity() {
        val intent = Intent(this, com.qb.browser.ui.history.HistoryActivity::class.java)
        historyActivityLauncher.launch(intent)
    }

    private fun setupAddressBar() {
        // Handle go button click
        goButton.setOnClickListener {
            val url = addressBar.text.toString().trim()
            if (url.isNotEmpty()) {
                handleUrlInput(url)
            } else {
                Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show()
            }
        }

        // Handle enter key press in address bar
        addressBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                val url = addressBar.text.toString().trim()
                if (url.isNotEmpty()) {
                    handleUrlInput(url)
                } else {
                    Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }
    }

    private fun handleUrlInput(inputUrl: String) {
        var url = inputUrl

        // Add https:// if no protocol is specified
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            // Check if it looks like a domain (contains a dot and no spaces)
            url = if (url.contains(".") && !url.contains(" ")) {
                "https://$url"
            } else {
                // Treat as search query
                "https://www.google.com/search?q=${Uri.encode(url)}"
            }
        }

        // Show loading state
        goButton.isEnabled = false
        goButton.alpha = 0.5f

        // Clear the address bar
        addressBar.text.clear()

        // Hide keyboard
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(addressBar.windowToken, 0)

        // Open URL in bubble
        startBubbleServiceWithUrl(url)

        // Provide user feedback
        Toast.makeText(this, "Opening in bubble...", Toast.LENGTH_SHORT).show()

        // Reset button state after a short delay
        goButton.postDelayed({
            goButton.isEnabled = true
            goButton.alpha = 1.0f
        }, 1000)

        Log.d(TAG, "Opening URL in bubble: $url")
    }



    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                try {
                    // Use the direct class reference instead of Class.forName
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                } catch (e: Exception) {
                    Log.e(TAG, "Error opening settings", e)
                    Toast.makeText(this, "Could not open settings", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.menu_history -> {
                openHistoryActivity()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        Log.d(TAG, "onNewIntent called with intent: ${intent?.action}")
        super.onNewIntent(intent)

        if (intent == null) return

        // Check if this is a return from authentication
        val data = intent.data
        if (data != null && data.scheme == "qbbrowser" && data.host == "auth-callback") {
            Log.d(TAG, "Received authentication callback: $data")
            handleAuthenticationCallback(data)
            return
        }

        // Always update the intent
        setIntent(intent)

        // If it's a link sharing intent, handle it without showing the UI
        if (isLinkSharingIntent(intent)) {
            Log.d(TAG, "Link sharing intent detected in onNewIntent, handling without UI")
            handleLinkSharingIntent(intent)
            return
        }
    }

    /**
     * Handles the callback from Chrome Custom Tabs after authentication
     */
    private fun handleAuthenticationCallback(uri: Uri) {
        Log.d(TAG, "Handling authentication callback: $uri")

        // Use the AuthenticationHandler to handle the return
        val handled = AuthenticationHandler.handleAuthenticationReturn(uri)

        if (handled) {
            Log.d(TAG, "Authentication callback handled successfully")
            // Minimize the app after handling the callback
            moveTaskToBack(true)
        } else {
            Log.e(TAG, "Failed to handle authentication callback")
            Toast.makeText(this, "Failed to complete authentication", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Checks if the intent is for link sharing (ACTION_SEND or ACTION_VIEW)
     */
    private fun isLinkSharingIntent(intent: Intent?): Boolean {
        return intent?.action == Intent.ACTION_SEND || intent?.action == Intent.ACTION_VIEW
    }

    /**
     * Handles link sharing intents by starting the bubble service and moving the activity to background
     * This allows the activity to receive multiple share intents without crashing
     */
    private fun handleLinkSharingIntent(intent: Intent?) {
        if (intent == null) return

        Log.d(TAG, "handleLinkSharingIntent | Received intent: ${intent.action}, data: ${intent.extras}")
        Log.d(TAG, "Activity will be moved to background instead of finishing to support multiple shares")

        when (intent.action) {
            Intent.ACTION_SEND -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                Log.d(TAG, "Received shared text: $sharedText")
                if (sharedText != null) {
                    // Start bubble service with the URL
                    if (checkPermissionsAndStartBubbleWithUrl(sharedText)) {
                        // Move task to back instead of finishing the activity
                        // This allows the activity to receive future share intents
                        moveTaskToBack(true)
                    }
                }
            }

            Intent.ACTION_VIEW -> {
                val url = intent.data?.toString()
                Log.d(TAG, "Received view URL: $url")
                if (url != null) {
                    // Start bubble service with the URL
                    if (checkPermissionsAndStartBubbleWithUrl(url)) {
                        // Move task to back instead of finishing the activity
                        // This allows the activity to receive future share intents
                        moveTaskToBack(true)
                    }
                }
            }
        }
    }

    /**
     * Checks permissions and starts the bubble service with a URL
     * Returns true if successful, false if permissions are needed
     */
    private fun checkPermissionsAndStartBubbleWithUrl(url: String): Boolean {
        // Check overlay permission
        if (!Settings.canDrawOverlays(this)) {
            // Show permission dialog
            requestOverlayPermission()
            // Save the URL to use after permission is granted
            settingsManager.saveLastSharedUrl(url)
            return false
        }

        // Start bubble service with URL
        startBubbleServiceWithUrl(url)
        return true
    }

    /**
     * Handles incoming intents for the main app (not link sharing)
     */
    private fun handleMainAppIntent() {
        if (!isBubbleServiceRunning()) {
            lifecycleScope.launch {
                delay(1000)
                checkPermissionsAndStartBubble()
            }
        }
    }

    /** Checks if the BubbleService is currently running. */
    private fun isBubbleServiceRunning(): Boolean {
        return BubbleService.isRunning()
    }

    /** Checks required permissions and starts the BubbleService if permissions are granted. */
    private fun checkPermissionsAndStartBubble(): Boolean {
        // Only check for overlay permission as it's essential
        if (!Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
            return false
        }

        // Start the service regardless of notification permission
        startBubbleService()
        return true
    }

    /** Requests overlay permission to draw bubbles over other apps. */
    private fun requestOverlayPermission() {
        try {
            val overlayIntent =
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    data = "package:$packageName".toUri()
                }
            startActivity(overlayIntent)
            Toast.makeText(
                this,
                "Please allow drawing over other apps to use the bubble browser",
                Toast.LENGTH_LONG
            )
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting overlay permission", e)
            Toast.makeText(this, "Could not open overlay permission settings", Toast.LENGTH_LONG)
                .show()
        }
    }

    // Notification permission rationale dialog removed as notifications are now optional

    override fun onResume() {
        super.onResume()

        // Check if we have a saved URL from a previous permission request
        val lastSharedUrl = settingsManager.getLastSharedUrl()
        if (lastSharedUrl != null && Settings.canDrawOverlays(this)
        ) {
            // Start bubble with the saved URL
            startBubbleServiceWithUrl(lastSharedUrl)
            // Clear the saved URL
            settingsManager.clearLastSharedUrl()
            // Move task to back if it was started from a link sharing intent
            if (isLinkSharingIntent(intent)) {
                moveTaskToBack(true)
                return
            }
        } else if (Settings.canDrawOverlays(this) && !isBubbleServiceRunning()
        ) {
            startBubbleService()
        }
    }


}