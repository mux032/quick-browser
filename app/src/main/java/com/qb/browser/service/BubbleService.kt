package com.qb.browser.service

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.qb.browser.MainActivity
import com.qb.browser.QBApplication
import com.qb.browser.R
import com.qb.browser.model.Bubble
import com.qb.browser.ui.BubbleAnimator
import com.qb.browser.ui.BubbleView
import com.qb.browser.util.SettingsManager
import com.qb.browser.viewmodel.BubbleViewModel
import com.qb.browser.viewmodel.WebViewModel
import com.qb.browser.NotificationPermissionActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Service managing floating bubbles
 */
class BubbleService : LifecycleService() {
    
    private lateinit var windowManager: WindowManager
    private lateinit var bubbleViewModel: BubbleViewModel
    private lateinit var webViewModel: WebViewModel
    private lateinit var settingsManager: SettingsManager
    private lateinit var bubbleAnimator: BubbleAnimator
    
    private val bubbles = ConcurrentHashMap<String, BubbleView>()
    private val bubbleModels = ConcurrentHashMap<String, Bubble>()
    private val serviceJob = SupervisorJob()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isUpdatingBubblesList = false
    private var isInitialized = false
    private val pendingIntents = mutableListOf<Intent>()
    private var mainBubbleId: String? = null
    private var activeBubbleId: String? = null
    private var isShowingAllBubbles = false
    private var isExpanded = false
    
    companion object {
        private const val TAG = "BubbleService"
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "bubble_service_channel"
        
        const val ACTION_START_BUBBLE = "com.qb.browser.START_BUBBLE"
        const val ACTION_OPEN_URL = "com.qb.browser.OPEN_URL"
        const val ACTION_CLOSE_BUBBLE = "com.qb.browser.CLOSE_BUBBLE"
        const val ACTION_SAVE_OFFLINE = "com.qb.browser.SAVE_OFFLINE"
        const val ACTION_OPEN_BUBBLE = "com.qb.browser.OPEN_BUBBLE"
        const val ACTION_ACTIVATE_BUBBLE = "com.qb.browser.ACTIVATE_BUBBLE"
        const val ACTION_TOGGLE_BUBBLES = "com.qb.browser.TOGGLE_BUBBLES"
        const val ACTION_SAVE_POSITION = "com.qb.browser.SAVE_POSITION"
        
        const val EXTRA_URL = "extra_url"
        const val EXTRA_BUBBLE_ID = "extra_bubble_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_X = "extra_x"
        const val EXTRA_Y = "extra_y"
        
        // Default URLs
        private const val DEFAULT_URL = "https://www.google.com"
        
        // Positioning constants
        private const val SCREEN_EDGE_MARGIN_DP = 16
        private const val BUBBLE_SPACING_DP = 80
        private const val BUBBLE_SIZE_DP = 56 // Fixed bubble size for all bubbles

        // Service state tracking
        @Volatile
        private var isServiceRunning = false
        
        fun isRunning(): Boolean = isServiceRunning
    }

    init {
        // Set up crash handler to ensure service state is properly reset
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception in thread $thread", throwable)
            isServiceRunning = false
            // Chain to default handler
            Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(thread, throwable)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "BubbleService onCreate()")
        
        try {
            // Update service running state
            isServiceRunning = true
            
            // First initialize core components
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            settingsManager = SettingsManager.getInstance(this)
            bubbleAnimator = BubbleAnimator(this)
            
            // Then create notification channel and start foreground
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, createNotification())
            Log.d(TAG, "Foreground service established")
            
            // Initialize ViewModels after core components
            val app = application as QBApplication
            bubbleViewModel = app.bubbleViewModel
            webViewModel = app.webViewModel
            
            // Set up observers 
            setupWebViewModelObservers()
            
            // Mark initialization as complete
            isInitialized = true
            Log.d(TAG, "Service initialized")
            
            // Process any pending intents
            processPendingIntents()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate: ${e.message}", e)
            isServiceRunning = false
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStartCommand received intent: ${intent?.action}")
        
        try {
            if (intent != null) {
                if (!isInitialized) {
                    Log.d(TAG, "Service not initialized, queuing intent")
                    pendingIntents.add(intent)
                } else {
                    Log.d(TAG, "Processing intent immediately")
                    processIntent(intent)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStartCommand: ${e.message}", e)
        }
        
        return START_STICKY
    }
    
    private fun processPendingIntents() {
        pendingIntents.forEach { intent ->
            processIntent(intent)
        }
        pendingIntents.clear()
    }
    
    private fun processIntent(intent: Intent) {
        when (intent.action) {
            ACTION_START_BUBBLE -> {
                if (mainBubbleId == null) {
                    createMainBubble()
                }
                
                // Don't create a new bubble for default URL
                val url = intent.getStringExtra(EXTRA_URL)
                if (url != null && url != DEFAULT_URL) {
                    createBubble(url)
                }
            }
            ACTION_OPEN_URL -> {
                val url = intent.getStringExtra(EXTRA_URL)
                if (url != null) {
                    // Ensure main bubble exists
                    if (mainBubbleId == null) {
                        createMainBubble()
                    }
                    // Create new bubble for URL
                    createBubble(url)
                    // Make the new bubble active immediately
                    activeBubbleId = url
                }
            }
            ACTION_CLOSE_BUBBLE -> {
                val bubbleId = intent.getStringExtra(EXTRA_BUBBLE_ID)
                Log.d(TAG, "Closing bubble: $bubbleId")
                bubbleId?.let { id -> closeBubble(id) }
            }
            ACTION_SAVE_OFFLINE -> {
                val bubbleId = intent.getStringExtra(EXTRA_BUBBLE_ID)
                val url = intent.getStringExtra(EXTRA_URL)
                Log.d(TAG, "Saving offline: bubble=$bubbleId, url=$url")
                if (bubbleId != null && url != null) {
                    savePageForOffline(bubbleId, url)
                }
            }
            ACTION_OPEN_BUBBLE -> {
                val bubbleId = intent.getStringExtra(EXTRA_BUBBLE_ID)
                Log.d(TAG, "Bringing bubble to front: $bubbleId")
                bubbleId?.let { id -> bringBubbleToFront(id) }
            }
            ACTION_ACTIVATE_BUBBLE -> {
                val bubbleId = intent.getStringExtra(EXTRA_BUBBLE_ID)
                bubbleId?.let { setActiveBubble(it) }
            }
            ACTION_SAVE_POSITION -> {
                val x = intent.getIntExtra(EXTRA_X, -1)
                val y = intent.getIntExtra(EXTRA_Y, -1)
                if (x != -1 && y != -1) {
                    settingsManager.saveMainBubblePosition(x, y)
                }
            }
        }
    }

    private fun createMainBubble() {
        val bubbleId = "main_bubble"
        mainBubbleId = bubbleId
        activeBubbleId = bubbleId

        lifecycleScope.launch(Dispatchers.Main) {
            try {
                val bubbleView = BubbleView(this@BubbleService, bubbleId, DEFAULT_URL).apply {
                    setOnClickListener { 
                        toggleAllBubbles()
                    }
                    setOnCloseListener { 
                        // Close all bubbles when main bubble is closed
                        bubbles.keys.toList().forEach { closeBubble(it) }
                    }
                }

                // Get saved position or use default
                val (savedX, savedY) = settingsManager.getMainBubblePosition()
                val screenWidth = resources.displayMetrics.widthPixels
                val margin = dpToPx(SCREEN_EDGE_MARGIN_DP)
                val x = if (savedX != -1) savedX else screenWidth - dpToPx(BUBBLE_SIZE_DP) - margin
                val y = if (savedY != -1) savedY else margin

                val params = createBubbleParams(x, y)

                windowManager.addView(bubbleView, params)

                // Store bubble
                bubbles[bubbleId] = bubbleView
                bubbleModels[bubbleId] = Bubble(
                    id = bubbleId,
                    url = DEFAULT_URL,
                    title = "All Tabs",
                    favicon = null
                )

                // Animate appearance
                if (settingsManager.areBubbleAnimationsEnabled()) {
                    bubbleAnimator.animateAppear(bubbleView)
                } else {
                    bubbleView.visibility = View.VISIBLE
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error creating main bubble", e)
            }
        }
    }

    private fun toggleAllBubbles() {
        isShowingAllBubbles = !isShowingAllBubbles
        if (isShowingAllBubbles) {
            showAllBubblesHorizontally()
        } else {
            hideAllBubblesExceptMain()
        }
    }

    private fun showAllBubblesHorizontally() {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val expandedBubbleSize = dpToPx(settingsManager.getExpandedBubbleSize())
        val margin = dpToPx(SCREEN_EDGE_MARGIN_DP)
        val spacing = dpToPx(16)
        
        // Calculate how many bubbles can fit in a row
        val availableWidth = screenWidth - margin * 2
        val maxBubblesPerRow = (availableWidth / (expandedBubbleSize + spacing)).coerceAtLeast(1)

        // Position main bubble at top-left
        bubbles[mainBubbleId]?.let { mainBubble ->
            val params = mainBubble.layoutParams as WindowManager.LayoutParams
            params.x = margin
            params.y = margin
            params.width = expandedBubbleSize
            params.height = expandedBubbleSize
            windowManager.updateViewLayout(mainBubble, params)
        }

        // Position other bubbles in a grid
        var currentX = margin + expandedBubbleSize + spacing
        var currentY = margin
        var bubbleCount = 0

        bubbles.forEach { (id, bubbleView) ->
            if (id != mainBubbleId) {
                val params = bubbleView.layoutParams as WindowManager.LayoutParams
                params.width = expandedBubbleSize
                params.height = expandedBubbleSize
                params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

                // Start a new row if we've reached the edge
                if (bubbleCount > 0 && bubbleCount % maxBubblesPerRow == 0) {
                    currentX = margin
                    currentY += expandedBubbleSize + spacing
                }

                params.x = currentX
                params.y = currentY
                windowManager.updateViewLayout(bubbleView, params)
                bubbleView.visibility = View.VISIBLE

                currentX += expandedBubbleSize + spacing
                bubbleCount++
            }
        }
    }

    private fun hideAllBubblesExceptMain() {
        bubbles.forEach { (id, bubbleView) ->
            if (id != mainBubbleId) {
                bubbleView.visibility = View.GONE
            }
        }
    }

    private fun setActiveBubble(bubbleId: String) {
        if (activeBubbleId == bubbleId && isExpanded) {
            // If clicking the same active bubble, minimize all and show main bubble
            minimizeAllBubbles()
            return
        }

        // Deactivate previous bubble if any
        activeBubbleId?.let { oldId ->
            bubbles[oldId]?.let { oldBubble ->
                // Reset to small floating bubble
                val params = oldBubble.layoutParams as WindowManager.LayoutParams
                val bubbleSize = dpToPx(BUBBLE_SIZE_DP)
                params.width = bubbleSize
                params.height = bubbleSize
                params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                windowManager.updateViewLayout(oldBubble, params)
                oldBubble.setInactive()
            }
        }

        // Activate new bubble
        bubbles[bubbleId]?.let { bubbleView ->
            bubbleView.setActive()
            activeBubbleId = bubbleId
            isExpanded = true

            // Make it full screen
            val params = bubbleView.layoutParams as WindowManager.LayoutParams
            params.width = WindowManager.LayoutParams.MATCH_PARENT
            params.height = WindowManager.LayoutParams.MATCH_PARENT
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                          WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            windowManager.updateViewLayout(bubbleView, params)
        }
    }

    private fun minimizeAllBubbles() {
        isExpanded = false
        isShowingAllBubbles = false
        activeBubbleId = null

        // Get saved main bubble position
        val (savedX, savedY) = settingsManager.getMainBubblePosition()
        val screenWidth = resources.displayMetrics.widthPixels
        val bubbleSize = dpToPx(BUBBLE_SIZE_DP)

        bubbles.forEach { (id, bubbleView) ->
            val params = bubbleView.layoutParams as WindowManager.LayoutParams
            params.width = bubbleSize
            params.height = bubbleSize
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

            if (id == mainBubbleId) {
                // Restore main bubble to its last position
                if (savedX != -1 && savedY != -1) {
                    params.x = savedX
                    params.y = savedY
                } else {
                    // Default position if no saved position
                    params.x = screenWidth - bubbleSize - dpToPx(SCREEN_EDGE_MARGIN_DP)
                    params.y = dpToPx(SCREEN_EDGE_MARGIN_DP)
                }
            }
            
            windowManager.updateViewLayout(bubbleView, params)
            bubbleView.setInactive()

            // Hide all except main bubble
            if (id != mainBubbleId) {
                bubbleView.visibility = View.GONE
            }
        }
    }

    private fun repositionBubbles() {
        val screenWidth = resources.displayMetrics.widthPixels
        val margin = dpToPx(SCREEN_EDGE_MARGIN_DP)
        val spacing = dpToPx(BUBBLE_SPACING_DP)
        var currentY = margin

        bubbles.forEach { (_, bubbleView) ->
            val params = bubbleView.layoutParams as WindowManager.LayoutParams
            params.x = screenWidth - dpToPx(settingsManager.getBubbleSize()) - margin
            params.y = currentY
            windowManager.updateViewLayout(bubbleView, params)
            currentY += spacing
        }
    }

    private fun createBubble(url: String) {
        val bubbleId = System.currentTimeMillis().toString()
        Log.d(TAG, "Creating bubble for URL: $url with ID: $bubbleId")

        lifecycleScope.launch(Dispatchers.Main) {
            try {
                Log.d(TAG, "Checking window manager permissions")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this@BubbleService)) {
                    Log.e(TAG, "Overlay permission not granted - cannot create bubble")
                    return@launch
                }
                val bubbleView = BubbleView(this@BubbleService, bubbleId, url).apply {
                    setOnClickListener {
                        if (activeBubbleId == bubbleId && isExpanded) {
                            minimizeAllBubbles()
                        } else {
                            setActiveBubble(bubbleId)
                        }
                    }
                    setOnCloseListener { 
                        closeBubble(bubbleId) 
                    }
                }

                val params = createBubbleParams(
                    resources.displayMetrics.widthPixels - dpToPx(BUBBLE_SIZE_DP) - dpToPx(SCREEN_EDGE_MARGIN_DP),
                    dpToPx(SCREEN_EDGE_MARGIN_DP)
                )

                windowManager.addView(bubbleView, params)
                bubbles[bubbleId] = bubbleView
                bubbleModels[bubbleId] = Bubble(
                    id = bubbleId,
                    url = url,
                    title = "",
                    favicon = null
                )

                // Load webpage immediately
                webViewModel.loadUrl(bubbleId, url)
                
                // Make this bubble active
                setActiveBubble(bubbleId)
                
                // Update bubble list
                updateBubblesListInViews()

            } catch (e: Exception) {
                Log.e(TAG, "Error creating bubble", e)
            }
        }
    }
    
    /**
     * Set up observers for bubble-specific data
     */
    private fun observeBubbleData(bubbleId: String, bubbleView: BubbleView) {
        // Observe favicon changes
        webViewModel.getFavicon(bubbleId).observe(this@BubbleService) { favicon ->
            favicon?.let { icon ->
                bubbleView.updateFavicon(icon)
                bubbleModels[bubbleId]?.let { bubble ->
                    bubbleModels[bubbleId] = bubble.copy(favicon = icon)
                }
                updateBubblesListInViews()
            }
        }
        
        // Observe loading progress
        webViewModel.getLoadingProgress(bubbleId).observe(this@BubbleService) { loadingProgress ->
            bubbleView.updateProgress(loadingProgress)
        }
    }
    
    /**
     * Update the bubbles list in all active bubble views
     */
    private fun updateBubblesListInViews() {
        if (isUpdatingBubblesList) return
        isUpdatingBubblesList = true
        
        try {
            val bubblesList = ArrayList<Bubble>()
            bubbleModels.values.forEach { model ->
                bubblesList.add(model)
            }
            
            bubbles.values.forEach { view ->
                view.updateBubblesList(bubblesList)
            }
        } finally {
            isUpdatingBubblesList = false
        }
    }
    
    /**
     * Bring a specific bubble to the front
     */
    private fun bringBubbleToFront(bubbleId: String) {
        bubbles[bubbleId]?.let { bubbleView ->
            try {
                // Update the layout params to bring to front
                val params = bubbleView.layoutParams as WindowManager.LayoutParams
                windowManager.updateViewLayout(bubbleView, params)
                
                // Update last accessed time
                bubbleModels[bubbleId]?.let { bubble ->
                    bubbleModels[bubbleId] = bubble.copy(lastAccessed = System.currentTimeMillis())
                }
                
                // Animate attention
                if (settingsManager.areBubbleAnimationsEnabled()) {
                    bubbleAnimator.animatePulse(bubbleView, 1)
                }
                
                // Update the tabs list in all bubbles to reflect selection
                updateBubblesListInViews()
            } catch (e: Exception) {
                // Ignore if bubble was already removed
            }
        }
    }
    
    /**
     * Close a bubble with animation
     */
    private fun closeBubble(bubbleId: String) {
        bubbles[bubbleId]?.let { bubbleView ->
            if (settingsManager.areBubbleAnimationsEnabled()) {
                // Animate disappearance then remove
                bubbleAnimator.animateDisappear(bubbleView, onEnd = {
                    removeBubble(bubbleId)
                })
            } else {
                // Remove immediately
                removeBubble(bubbleId)
            }
        }
    }
    
    /**
     * Remove a bubble from the window manager
     */
    private fun removeBubble(bubbleId: String) {
        try {
            bubbles[bubbleId]?.let {
                windowManager.removeView(it)
                bubbles.remove(bubbleId)
                bubbleModels.remove(bubbleId)
                webViewModel.closePage(bubbleId)
                
                // Update tabs list in remaining bubbles
                updateBubblesListInViews()
            }
            
            // If no bubbles are left, stop the service
            if (bubbles.isEmpty()) {
                stopSelf()
            }
        } catch (e: Exception) {
            // Ignore if bubble was already removed
        }
    }
    
    /**
     * Save a web page for offline access
     */
    private fun savePageForOffline(bubbleId: String, url: String) {
        val title = bubbleViewModel.getTitle(bubbleId).value ?: "Untitled"
        OfflinePageService.startService(this, url, title)

        bubbles[bubbleId]?.let { bubble ->
            val messageView = bubble.findViewById<TextView>(R.id.save_message)
            if (messageView != null) {
                messageView.text = getString(R.string.saving_page_offline)
                messageView.visibility = View.VISIBLE
                
                lifecycleScope.launch {
                    delay(3000)
                    messageView.visibility = View.GONE
                }
            }
        }
    }
    
    /**
     * Convert dp to pixels
     */
    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }
    
    /**
     * Create the notification channel for Android O+
     */
    private fun createNotificationChannel() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val name = getString(R.string.app_name)
                val descriptionText = "Running in background"
                val importance = NotificationManager.IMPORTANCE_LOW
                val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    setShowBadge(false)
                }
                
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Notification channel created successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create notification channel", e)
            // Try to proceed anyway since the service can still run without the channel
        }
    }
    
    /**
     * Create the persistent notification
     */
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) 
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            else 
                PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Browser is running in bubble mode")
            .setSmallIcon(R.drawable.ic_globe)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Update service running state
        isServiceRunning = false
        
        // Remove all bubbles
        bubbles.keys.toList().forEach { removeBubble(it) }
        
        // Cancel coroutines
        serviceJob.cancel()
    }

    /**
     * Request overlay permission
     */
    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivity(intent)
    }

    /**
     * Start the bubble service
     */
    private fun startBubbleService() {
        val intent = Intent(this, BubbleService::class.java).apply {
            action = ACTION_START_BUBBLE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    /**
     * Check required permissions and then start the bubble service
     */
    private fun checkPermissionsAndStartBubble(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
            false
        } else {
            startBubbleService()
            true
        }
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val intent = Intent(this, NotificationPermissionActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
        }
    }

    private fun notifyWithPermissionCheck(notificationId: Int, notification: Notification) {
        if (hasNotificationPermission()) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(notificationId, notification)
        } else {
            requestNotificationPermission()
        }
    }

    /**
     * Set up observers for web content updates
     */
    private fun setupWebViewModelObservers() {
        webViewModel.getAllPageTitles().observe(this@BubbleService) { titleMap ->
            // Update bubble models with titles
            titleMap.forEach { (id, pageTitle) ->
                bubbleModels[id]?.let { bubble ->
                    bubbleModels[id] = bubble.copy(title = pageTitle)
                }
            }
            updateBubblesListInViews()
        }
    }

    private fun createBubbleParams(x: Int, y: Int): WindowManager.LayoutParams {
        val bubbleSize = dpToPx(BUBBLE_SIZE_DP)
        return WindowManager.LayoutParams(
            bubbleSize, // Fixed width
            bubbleSize, // Fixed height
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O || Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE // Pre-M fallback that doesn't require permissions
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            this.x = x
            this.y = y
        }
    }

    // Add this method to save main bubble position when it's dragged
    private fun saveMainBubblePosition(x: Int, y: Int) {
        // Check if this is the main bubble by comparing IDs
        if (mainBubbleId != null && !isExpanded) {
            settingsManager.saveMainBubblePosition(x, y)
        }
    }
}
