package com.quick.browser.presentation.ui.main

import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
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
    }

    private val viewModel: MainViewModel by viewModels()

    private lateinit var addressBar: EditText
    private lateinit var menuButton: ImageButton
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar

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
        if (!checkOverlayPermission()) {
            Logger.d(TAG, "Overlay permission not granted, returning early")
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
            Logger.d(TAG, "Successfully started BubbleService with URL: $url")
            // Removed moveTaskToBack(true) to keep the app in foreground
        } catch (e: Exception) {
            Logger.e(TAG, "Failed to start bubble service", e)
            Toast.makeText(this, "Failed to start bubble service", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set status bar icon color to dark for better visibility on light background
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
        }

        // Initialize views
        addressBar = findViewById(R.id.address_bar)
        menuButton = findViewById(R.id.menu_button)
        toolbar = findViewById(R.id.toolbar)

        setupAddressBar()
        setupMenuButton()
        setupKeyboardVisibilityListener()
        
        // Handle incoming intent if it's a link sharing intent
        if (isLinkSharingIntent(intent)) {
            Logger.d(TAG, "Link sharing intent detected in onCreate, handling without UI")
            handleLinkSharingIntent(intent)
        } else {
            handleMainAppIntent()
        }
    }

    private fun openHistoryActivity() {
        val intent = Intent(this, HistoryActivity::class.java)
        historyActivityLauncher.launch(intent)
    }

    private fun setupAddressBar() {
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
        
        // Handle address bar focus change
        addressBar.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // Show keyboard
                addressBar.post {
                    val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(addressBar, InputMethodManager.SHOW_IMPLICIT)
                }
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

        // Check if it looks like a valid URL or domain
        val isValidUrl = url.startsWith("http://") || url.startsWith("https://") || 
                         (url.contains(".") && !url.contains(" ") && !url.contains("\\"))

        if (!isValidUrl) {
            // Treat as search query
            url = "https://www.google.com/search?q=${Uri.encode(url)}"
        } else if (!url.startsWith("http://") && !url.startsWith("https://")) {
            // Add https:// if no protocol is specified but it looks like a domain
            url = "https://$url"
        }

        // Clear the address bar
        addressBar.text.clear()

        // Hide keyboard
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(addressBar.windowToken, 0)

        // Open URL in bubble
        startBubbleServiceWithUrl(url)

        // Provide user feedback
        Toast.makeText(this, "Opening in bubble...", Toast.LENGTH_SHORT).show()

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
        
        // Handle main app intent for other cases
        handleMainAppIntent()
    }

    /**
     * Handles the callback from Chrome Custom Tabs after authentication
     */
    private fun handleAuthenticationCallback(uri: Uri) {
        Logger.d(TAG, "Handling authentication callback: $uri")

        // Use the AuthenticationHandler to handle the return
        val handled = AuthenticationService.handleAuthenticationReturn(uri)

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
        val result = intent?.action == Intent.ACTION_SEND || intent?.action == Intent.ACTION_VIEW
        Logger.d(TAG, "isLinkSharingIntent: action=${intent?.action}, result=$result")
        return result
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
                        // Move task to back after starting the service if permissions were already granted
                        Logger.d(TAG, "Moving task to back after starting bubble service")
                        moveTaskToBack(true)
                    } else {
                        Logger.d(TAG, "Permissions not granted, will start bubble after permission")
                    }
                } else {
                    Logger.d(TAG, "No shared text found in ACTION_SEND intent")
                }
            }

            Intent.ACTION_VIEW -> {
                val url = intent.data?.toString()
                Logger.d(TAG, "Received view URL: $url")
                if (url != null) {
                    // Start bubble service with the URL
                    if (checkPermissionsAndStartBubbleWithUrl(url)) {
                        // Move task to back after starting the service if permissions were already granted
                        Logger.d(TAG, "Moving task to back after starting bubble service")
                        moveTaskToBack(true)
                    } else {
                        Logger.d(TAG, "Permissions not granted, will start bubble after permission")
                    }
                } else {
                    Logger.d(TAG, "No URL found in ACTION_VIEW intent")
                }
            }
            
            else -> {
                Logger.d(TAG, "Unsupported intent action: ${intent.action}")
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
            // Save the URL to use after permission is granted
            pendingUrl = url
            // Show permission dialog
            requestOverlayPermission()
            return false
        }

        // Start bubble service with URL
        startBubbleServiceWithUrl(url)
        // Move task to back after starting the service
        moveTaskToBack(true)
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

    /** Checks required permissions for overlay */
    private fun checkOverlayPermission(): Boolean {
        // Only check for overlay permission as it's essential
        val canDrawOverlays = Settings.canDrawOverlays(this)
        Logger.d(TAG, "checkOverlayPermission: canDrawOverlays=$canDrawOverlays")
        if (!canDrawOverlays) {
            requestOverlayPermission()
            return false
        }
        return true
    }

    /** Checks required permissions and starts the BubbleService if permissions are granted. */
    private fun checkPermissionsAndStartBubble(): Boolean {
        // Only check for overlay permission as it's essential
        if (!checkOverlayPermission()) {
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
            Logger.d(TAG, "Requested overlay permission")
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

            // Update toolbar position based on keyboard visibility
            val layoutParams = toolbar.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
            if (keypadHeight > screenHeight * 0.15) {
                // Keyboard is visible, position toolbar above it
                layoutParams.bottomMargin = keypadHeight
                // Hide menu button when keyboard is visible
                menuButton.visibility = View.GONE
            } else {
                // Keyboard is hidden, toolbar sits at bottom of screen
                layoutParams.bottomMargin = 0
                // Show menu button when keyboard is hidden
                menuButton.visibility = View.VISIBLE
            }
            toolbar.layoutParams = layoutParams
        }
        
        // Set up touch listener to hide keyboard when clicking outside
        window.decorView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                val view = currentFocus
                if (view is EditText) {
                    val outRect = Rect()
                    view.getGlobalVisibleRect(outRect)
                    if (!outRect.contains(event.rawX.toInt(), event.rawY.toInt())) {
                        view.clearFocus()
                        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(view.windowToken, 0)
                    }
                }
            }
            false
        }
    }

    // Store the URL that needs to be opened after permission is granted
    private var pendingUrl: String? = null
    
    override fun onResume() {
        super.onResume()
        Logger.d(TAG, "onResume called")

        // Check if we have a saved URL from a previous permission request
        pendingUrl?.let { url ->
            Logger.d(TAG, "Found pending URL: $url")
            if (Settings.canDrawOverlays(this)) {
                Logger.d(TAG, "Overlay permission granted, starting bubble service with pending URL")
                // Permission granted, start the bubble service with the URL
                startBubbleServiceWithUrl(url)
                pendingUrl = null // Clear the pending URL
                moveTaskToBack(true) // Move to background
            } else {
                Logger.d(TAG, "Overlay permission still not granted")
            }
        }
    }
}