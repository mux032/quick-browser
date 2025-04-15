package com.qb.browser.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.qb.browser.R
import com.qb.browser.ui.OfflinePageActivity
import com.qb.browser.util.OfflinePageManager
import com.qb.browser.NotificationPermissionActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Service for saving pages for offline access in the background
 */
class OfflinePageService : Service() {
    private val TAG = "OfflinePageService"
    private val NOTIFICATION_ID = 2001
    private val CHANNEL_ID = "offline_page_channel"
    
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val isRunning = AtomicBoolean(false)
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val url = intent.getStringExtra(EXTRA_URL)
            val title = intent.getStringExtra(EXTRA_TITLE)
            val htmlContent = intent.getStringExtra(EXTRA_HTML_CONTENT)
            
            if (!url.isNullOrEmpty() && !title.isNullOrEmpty()) {
                if (!isRunning.get()) {
                    isRunning.set(true)
                    startForeground(NOTIFICATION_ID, createNotification(title, 0))
                    
                    if (htmlContent != null) {
                        // Save with provided HTML content
                        savePageWithHtmlContent(url, title, htmlContent, startId)
                    } else {
                        // Regular save by downloading the page
                        savePageForOffline(url, title, startId)
                    }
                } else {
                    Log.w(TAG, "Service is already saving a page, ignoring request")
                    stopSelf(startId)
                }
            } else {
                Log.e(TAG, "Missing URL or title in intent")
                stopSelf(startId)
            }
        } else {
            stopSelf(startId)
        }
        
        return START_NOT_STICKY
    }
    
    private fun savePageForOffline(url: String, title: String, startId: Int) {
        scope.launch {
            try {
                val offlinePageManager = OfflinePageManager.getInstance(this@OfflinePageService)
                
                val callback = object : OfflinePageManager.SaveCallback {
                    override fun onProgress(progress: Int, message: String) {
                        updateNotification(title, progress)
                    }
                    
                    override fun onComplete(pageId: String, filePath: String) {
                        // Show completion notification
                        val completeNotification = createCompletionNotification(title, pageId)
                        notifyWithPermissionCheck(NOTIFICATION_ID + 1, completeNotification)
                        
                        isRunning.set(false)
                        stopSelf(startId)
                    }
                    
                    override fun onError(errorMessage: String) {
                        val errorNotification = createErrorNotification(title, errorMessage)
                        notifyWithPermissionCheck(NOTIFICATION_ID + 2, errorNotification)
                        
                        isRunning.set(false)
                        stopSelf(startId)
                    }
                }
                
                // Since we don't have HTML content ready, we can use the WebView directly
                // For now, save an empty page as a placeholder - this needs to be fixed by integrating WebView
                offlinePageManager.savePageWithHtmlContent(url, title, "<html><body><p>Loading page content...</p></body></html>", callback)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving page: ${e.message}", e)
                isRunning.set(false)
                stopSelf(startId)
            }
        }
    }
    
    private fun savePageWithHtmlContent(url: String, title: String, htmlContent: String, startId: Int) {
        scope.launch {
            try {
                val offlinePageManager = OfflinePageManager.getInstance(this@OfflinePageService)
                
                val callback = object : OfflinePageManager.SaveCallback {
                    override fun onProgress(progress: Int, message: String) {
                        updateNotification(title, progress)
                    }
                    
                    override fun onComplete(pageId: String, filePath: String) {
                        // Show completion notification
                        val completeNotification = createCompletionNotification(title, pageId)
                        notifyWithPermissionCheck(NOTIFICATION_ID + 1, completeNotification)
                        
                        isRunning.set(false)
                        stopSelf(startId)
                    }
                    
                    override fun onError(errorMessage: String) {
                        val errorNotification = createErrorNotification(title, errorMessage)
                        notifyWithPermissionCheck(NOTIFICATION_ID + 2, errorNotification)
                        
                        isRunning.set(false)
                        stopSelf(startId)
                    }
                }
                
                // Save the page with the provided HTML content
                offlinePageManager.savePageWithHtmlContent(url, title, htmlContent, callback)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving page with HTML content: ${e.message}", e)
                isRunning.set(false)
                stopSelf(startId)
            }
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Offline Pages",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for saving offline pages"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(title: String, progress: Int): Notification {
        val intent = Intent(this, OfflinePageActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Saving page for offline")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
        
        if (progress > 0) {
            builder.setProgress(100, progress, false)
        } else {
            builder.setProgress(100, 0, true)
        }
        
        return builder.build()
    }
    
    private fun updateNotification(title: String, progress: Int) {
        val notification = createNotification(title, progress)
        notifyWithPermissionCheck(NOTIFICATION_ID, notification)
    }
    
    private fun createCompletionNotification(title: String, pageId: String): Notification {
        val intent = Intent(this, OfflinePageActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Page saved for offline")
            .setContentText(title)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
    }
    
    private fun createErrorNotification(title: String, errorMessage: String): Notification {
        val intent = Intent(this, OfflinePageActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Error saving page")
            .setContentText("$title - $errorMessage")
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
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

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_HTML_CONTENT = "extra_html_content"
        
        fun startService(context: Context, url: String, title: String) {
            val intent = Intent(context, OfflinePageService::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_TITLE, title)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun startServiceWithHtml(context: Context, url: String, title: String, htmlContent: String) {
            val intent = Intent(context, OfflinePageService::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_HTML_CONTENT, htmlContent)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}