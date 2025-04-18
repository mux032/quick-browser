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
 * - Custom classes like `BubbleService`, `SettingsManager`, `SettingsActivity`, and
 * `HistoryActivity`.
 */
package com.qb.browser

// import com.qb.browser.ui.HistoryActivity

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.qb.browser.service.BubbleService
import com.qb.browser.ui.SettingsActivity
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

        val intent =
                Intent(this, BubbleService::class.java).apply {
                    action = Constants.ACTION_CREATE_BUBBLE
                    putExtra(BubbleService.EXTRA_URL, url)
                }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                Log.e(TAG, "Its not great Build.VERSION.SDK_INT >= Build.VERSION_CODES.O")
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

        settingsManager = SettingsManager.getInstance(this)
        intent?.let { handleIntent(it) }

        findViewById<Button>(R.id.btn_start_bubble).setOnClickListener {
            checkPermissionsAndStartBubble()
        }

        findViewById<View>(R.id.btn_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // findViewById<View>(R.id.btn_history).setOnClickListener {
        //     startActivity(Intent(this, HistoryActivity::class.java))
        // }
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
        when (intent.action) {
            Intent.ACTION_SEND -> {
                intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedText ->
                    startBubbleServiceWithUrl(sharedText)
                }
            }
            Intent.ACTION_VIEW -> {
                intent.data?.toString()?.let { url -> startBubbleServiceWithUrl(url) }
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
}
