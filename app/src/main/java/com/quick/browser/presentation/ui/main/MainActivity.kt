package com.quick.browser.presentation.ui.main

import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.quick.browser.R
import com.quick.browser.presentation.ui.components.BaseActivity
import com.quick.browser.presentation.ui.history.HistoryActivity
import com.quick.browser.presentation.ui.saved.SavedArticlesActivity
import com.quick.browser.presentation.ui.settings.SettingsActivity
import com.quick.browser.service.AuthenticationService
import com.quick.browser.service.BubbleService
import com.quick.browser.utils.Constants
import com.quick.browser.utils.Logger
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : BaseActivity() {
    companion object {
        private const val TAG = "MainActivity"
        // Removed NOTIFICATION_PERMISSION constant as it's no longer needed
    }

    private val viewModel: MainViewModel by viewModels()

    private lateinit var addressBar: EditText
    private lateinit var goButton: ImageButton
    private lateinit var menuButton: ImageButton
    private lateinit var addressBarContainer: android.widget.LinearLayout

    // Activity result launcher for history activity
    private val historyActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedUrl = result.data?.getStringExtra(HistoryActivity.EXTRA_SELECTED_URL)
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
            Logger.e(TAG, "Failed to start bubble service", e)
            Toast.makeText(this, "Failed to start bubble service", Toast.LENGTH_SHORT).show()
        }
    }

    /** Starts the BubbleService with a specific URL to open in the quick browser. */
    private fun startBubbleServiceWithUrl(url: String) {
        if (!checkPermissionsAndStartBubble()) {
            return
        }

        Logger.d(TAG, "startBubbleServiceWithUrl: Starting service with URL: $url")
        val intent =
            Intent(this, BubbleService::class.java).apply {
                action = Constants.ACTION_CREATE_BUBBLE
                putExtra(Constants.EXTRA_URL, url)
            }

        try {
            startForegroundService(intent)
            // Removed moveTaskToBack(true) to keep the app in foreground
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to start bubble service", e)
            Toast.makeText(this, "Failed to start bubble service", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        addressBar = findViewById(R.id.address_bar)
        goButton = findViewById(R.id.go_button)
        menuButton = findViewById(R.id.menu_button)
        addressBarContainer = findViewById(R.id.address_bar_container)

        setupAddressBar()
        setupMenuButton()
        setupKeyboardVisibilityListener()
        handleMainAppIntent()
    }

    private fun openHistoryActivity() {
        val intent = Intent(this, HistoryActivity::class.java)
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

    private fun setupMenuButton() {
        menuButton.setOnClickListener {
            showPopupMenu()
        }
    }

    private fun showPopupMenu() {
        val popupMenu = PopupMenu(this, menuButton)
        popupMenu.menuInflater.inflate(R.menu.main_menu, popupMenu.menu)
        
        // Force icons to show in popup menu
        try {
            val field = popupMenu.javaClass.getDeclaredField("mPopup")
            field.isAccessible = true
            val menuPopupHelper = field.get(popupMenu)
            val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
            val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.java)
            setForceIcons.invoke(menuPopupHelper, true)
        } catch (e: Exception) {
            Logger.e(TAG, "Error forcing icons to show in popup menu", e)
        }
        
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_settings -> {
                    try {
                        val intent = Intent(this, SettingsActivity::class.java)
                        startActivity(intent)
                    } catch (e: Exception) {
                        Logger.e(TAG, "Error opening settings", e)
                        Toast.makeText(this, "Could not open settings", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                R.id.menu_history -> {
                    openHistoryActivity()
                    true
                }
                R.id.menu_saved_articles -> {
                    try {
                        val intent = Intent(this, SavedArticlesActivity::class.java)
                        startActivity(intent)
                    } catch (e: Exception) {
                        Logger.e(TAG, "Error opening saved articles", e)
                        Toast.makeText(this, "Could not open saved articles", Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
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
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
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

        Logger.d(TAG, "Opening URL in bubble: $url")
    }



    

    override fun onNewIntent(intent: Intent?) {
        Logger.d(TAG, "onNewIntent called with intent: ${intent?.action}")
        super.onNewIntent(intent)

        if (intent == null) return

        // Check if this is a return from authentication
        val data = intent.data
        if (data != null && data.scheme == "quick_browser" && data.host == "auth-callback") {
            Logger.d(TAG, "Received authentication callback: $data")
            handleAuthenticationCallback(data)
            return
        }

        // Always update the intent
        setIntent(intent)

        // If it's a link sharing intent, handle it without showing the UI
        if (isLinkSharingIntent(intent)) {
            Logger.d(TAG, "Link sharing intent detected in onNewIntent, handling without UI")
            handleLinkSharingIntent(intent)
            return
        }
    }

    /**
     * Handles the callback from Chrome Custom Tabs after authentication
     */
    private fun handleAuthenticationCallback(uri: Uri) {
        Logger.d(TAG, "Handling authentication callback: $uri")

        // Use the AuthenticationHandler to handle the return
        val handled = AuthenticationService.Companion.handleAuthenticationReturn(uri)

        if (handled) {
            Logger.d(TAG, "Authentication callback handled successfully")
            // Minimize the app after handling the callback
            moveTaskToBack(true)
        } else {
            Logger.e(TAG, "Failed to handle authentication callback")
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

        Logger.d(TAG, "handleLinkSharingIntent | Received intent: ${intent.action}, data: ${intent.extras}")
        Logger.d(TAG, "Activity will be moved to background instead of finishing to support multiple shares")

        when (intent.action) {
            Intent.ACTION_SEND -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                Logger.d(TAG, "Received shared text: $sharedText")
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
                Logger.d(TAG, "Received view URL: $url")
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
            // Note: We should use the ViewModel to manage this state
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
        return BubbleService.Companion.isRunning()
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
                "Please allow drawing over other apps to use the quick browser",
                Toast.LENGTH_LONG
            )
                .show()
        } catch (e: Exception) {
            Logger.e(TAG, "Error requesting overlay permission", e)
            Toast.makeText(this, "Could not open overlay permission settings", Toast.LENGTH_LONG)
                .show()
        }
    }

    // Notification permission rationale dialog removed as notifications are now optional

    private fun setupKeyboardVisibilityListener() {
        // Listen for keyboard visibility changes
        window.decorView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            window.decorView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = window.decorView.height
            val keypadHeight = screenHeight - rect.bottom

            // Update address bar container bottom margin to position it above keyboard
            val layoutParams = addressBarContainer.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            layoutParams.bottomMargin = if (keypadHeight > screenHeight * 0.15) {
                // Keyboard is visible, position above it
                keypadHeight + 24 // Add some padding
            } else {
                // Keyboard is hidden, use default margin
                24
            }
            addressBarContainer.layoutParams = layoutParams
        }
    }

    override fun onResume() {
        super.onResume()

        // Check if we have a saved URL from a previous permission request
        // Note: We should use the ViewModel to manage this state
        // For now, we'll keep the existing implementation
    }
}