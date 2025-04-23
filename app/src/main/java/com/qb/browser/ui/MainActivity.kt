/**
 * MainActivity.kt
 *
 * Summary: This activity serves as the main entry point for the Bubble Browser application. It
 * manages permissions, user interactions, and service operations for the floating bubble feature.
 * The activity performs the following tasks:
 *
 * 1. **Permission Management**:
 * ```
 *    - Requests overlay permission to draw bubbles over other apps.
 *    - Requests notification permission for Android 13+ devices to display notifications.
 *    - Provides rationale dialogs for permissions when required.
 * ```
 * 2. **Bubble Service Management**:
 * ```
 *    - Starts the BubbleService to display floating bubbles.
 *    - Handles intents to open URLs in the bubble browser.
 *    - Checks if the BubbleService is already running.
 * ```
 * 3. **UI Interaction**:
 * ```
 *    - Provides buttons to start the bubble browser, open settings, and view history.
 *    - Handles user actions like sharing URLs or opening links via intents.
 * ```
 * 4. **Lifecycle Management**:
 * ```
 *    - Ensures the BubbleService is started when the app resumes if permissions are granted.
 * ```
 * Dependencies:
 * - AndroidX libraries for lifecycle management and permissions.
 * - Kotlin Coroutines for asynchronous operations.
 * - Custom classes like `BubbleService`, `SettingsManager`, and `SettingsActivity`.
 */
package com.qb.browser.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.qb.browser.Constants
import com.qb.browser.R
import com.qb.browser.model.WebPage
import com.qb.browser.service.BubbleService
import com.qb.browser.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.qb.browser.util.SettingsManager
import com.qb.browser.util.BubbleIntentProcessor
import com.qb.browser.manager.BubbleManager
import com.qb.browser.db.WebPageDao
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val NOTIFICATION_PERMISSION = "android.permission.POST_NOTIFICATIONS"
    }

    private lateinit var settingsManager: SettingsManager
    private lateinit var bubbleIntentProcessor: BubbleIntentProcessor
    private lateinit var historyViewModel: HistoryViewModel
    private lateinit var historyAdapter: HistoryAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    
    private val permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    startBubbleService()
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }

    /** Starts the BubbleService to display floating bubbles. */
    private fun startBubbleService() {
        val intent =
                Intent(this, BubbleService::class.java).apply {
                    action = Constants.ACTION_CREATE_BUBBLE
                }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            moveTaskToBack(true)
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                Log.d(TAG, "Using startService for pre-Oreo devices")
                startService(intent)
            }
            moveTaskToBack(true)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start bubble service", e)
            Toast.makeText(this, "Failed to start bubble service", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set up toolbar
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false) // Hide default title

        settingsManager = SettingsManager.getInstance(this)
        intent?.let { handleIntent(it) }

        // Initialize ViewModel
        historyViewModel = ViewModelProvider(this)[HistoryViewModel::class.java]
        
        // Initialize views
        recyclerView = findViewById(R.id.history_recycler_view)
        emptyView = findViewById(R.id.empty_history_view)
        
        // Set up RecyclerView
        setupRecyclerView()
        
        // Observe history data
        observeHistoryData()
        
        // Automatically start bubble service when app opens
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && 
            Settings.canDrawOverlays(this) && 
            !isBubbleServiceRunning()) {
            checkPermissionsAndStartBubble()
        }
    }
    
    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter(
            onItemClick = { webPage ->
                openPageInBubble(webPage.url)
            }
        )
        
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            // Removed divider decoration to eliminate horizontal lines
            adapter = this@MainActivity.historyAdapter
        }
    }
    
    private fun observeHistoryData() {
        historyViewModel.getRecentPages(20).observe(this) { pages ->
            if (pages.isEmpty()) {
                recyclerView.visibility = View.GONE
                emptyView.visibility = View.VISIBLE
            } else {
                recyclerView.visibility = View.VISIBLE
                emptyView.visibility = View.GONE
                historyAdapter.submitList(pages)
            }
        }
    }
    
    private fun openPageInBubble(url: String) {
        startBubbleServiceWithUrl(url)
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
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent?.let { handleIntent(it) }
    }

    /**
     * Handles incoming intents to perform actions like opening URLs or starting the bubble browser.
     */
    private fun handleIntent(intent: Intent) {
        Log.d(TAG, "handleIntent | Received intent: ${intent.action}, data: ${intent.extras}")
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                Log.d(TAG, "Received shared text: $sharedText")
                if (sharedText != null) {
                    startBubbleServiceWithUrl(sharedText)
                }
            }
            Intent.ACTION_VIEW -> {
                val url = intent.data?.toString()
                Log.d(TAG, "Received view URL: $url")
                if (url != null) {
                    startBubbleServiceWithUrl(url)
                }
            }
            else -> {
                if (!isBubbleServiceRunning()) {
                    lifecycleScope.launch {
                        delay(1000)
                        checkPermissionsAndStartBubble()
                    }
                }
            }
        }
    }

    /** Checks if the BubbleService is currently running. */
    private fun isBubbleServiceRunning(): Boolean {
        return BubbleService.isRunning()
    }

    /** Checks required permissions and starts the BubbleService if permissions are granted. */
    private fun checkPermissionsAndStartBubble(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            requestOverlayPermission()
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, NOTIFICATION_PERMISSION) ==
                        PackageManager.PERMISSION_GRANTED -> {
                    startBubbleService()
                    return true
                }
                shouldShowRequestPermissionRationale(NOTIFICATION_PERMISSION) -> {
                    showNotificationPermissionRationale()
                    return false
                }
                else -> {
                    permissionLauncher.launch(NOTIFICATION_PERMISSION)
                    return false
                }
            }
        }

        startBubbleService()
        return true
    }

    /** Requests overlay permission to draw bubbles over other apps. */
    private fun requestOverlayPermission() {
        try {
            val overlayIntent =
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
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

    /** Shows a rationale dialog for notification permission. */
    private fun showNotificationPermissionRationale() {
        androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Notification Permission Required")
                .setMessage(
                        "The app needs notification permission to run in the background and show floating bubbles."
                )
                .setPositiveButton("Grant Permission") { _, _ ->
                    permissionLauncher.launch(NOTIFICATION_PERMISSION)
                }
                .setNegativeButton("Cancel", null)
                .show()
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        Settings.canDrawOverlays(this) &&
                        !isBubbleServiceRunning()
        ) {
            startBubbleService()
        }
    }
    
    // RecyclerView Adapter for History items
    private inner class HistoryAdapter(
        private val onItemClick: (WebPage) -> Unit
    ) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {
        
        private var items = listOf<WebPage>()
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
        
        fun submitList(newItems: List<WebPage>) {
            items = newItems.sortedByDescending { it.timestamp }
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_history, parent, false)
            return ViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val page = items[position]
            
            // Use a proper title or fallback to "Untitled Page"
            // Don't show URL as title
            val displayTitle = when {
                page.title.isEmpty() -> getString(R.string.untitled_page)
                page.title == page.url -> getString(R.string.untitled_page)
                else -> page.title
            }
            
            holder.titleTextView.text = displayTitle
            holder.urlTextView.text = page.url
            holder.dateTextView.text = dateFormat.format(Date(page.timestamp))
            
            // Set Offline indicator if available offline
            holder.offlineIndicator.visibility = if (page.isAvailableOffline) View.VISIBLE else View.GONE
            
            holder.itemView.setOnClickListener {
                onItemClick(page)
            }
        }
        
        override fun getItemCount() = items.size
        
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val titleTextView: TextView = itemView.findViewById(R.id.text_title)
            val urlTextView: TextView = itemView.findViewById(R.id.text_url)
            val dateTextView: TextView = itemView.findViewById(R.id.text_date)
            val offlineIndicator: View = itemView.findViewById(R.id.offline_indicator)
            val deleteButton: View = itemView.findViewById(R.id.btn_delete)
            
            init {
                // Hide delete button as we're just showing history
                deleteButton.visibility = View.GONE
            }
        }
    }
}