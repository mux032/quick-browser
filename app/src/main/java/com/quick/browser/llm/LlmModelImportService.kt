package com.quick.browser.llm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.IBinder
import com.quick.browser.R
import com.quick.browser.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import android.content.Context.RECEIVER_NOT_EXPORTED
import java.io.File
import java.io.FileOutputStream

class LlmModelImportService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val TAG = "LlmModelImportService"

    private val importReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.quick.browser.IMPORT_LLM_MODEL") {
                val uriString = intent.getStringExtra("IMPORT_MODEL_URI")
                if (uriString != null) {
                    importModelFromUri(Uri.parse(uriString))
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Logger.d(TAG, "LlmModelImportService created")
        
        // Create notification channel for Android O+
        createNotificationChannel()
        
        // Start foreground service
        startForeground(LLM_IMPORT_SERVICE_NOTIFICATION_ID, createNotification())
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(importReceiver, IntentFilter("com.quick.browser.IMPORT_LLM_MODEL"), RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(importReceiver, IntentFilter("com.quick.browser.IMPORT_LLM_MODEL"), RECEIVER_NOT_EXPORTED)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                LLM_IMPORT_SERVICE_CHANNEL_ID,
                "LLM Model Import Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Handles LLM model imports in the background"
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, LLM_IMPORT_SERVICE_CHANNEL_ID)
                .setContentTitle("LLM Model Import Service")
                .setContentText("Ready to import LLM models")
                .setSmallIcon(R.drawable.ic_ai)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("LLM Model Import Service")
                .setContentText("Ready to import LLM models")
                .setSmallIcon(R.drawable.ic_ai)
                .build()
        }
    }

    private fun importModelFromUri(uri: Uri) {
        Logger.d(TAG, "Starting model import from URI: $uri")
        
        serviceScope.launch {
            try {
                // Get file name and size
                val fileName = getFileNameFromUri(uri) ?: "imported_model.task"
                val fileSize = getFileSizeFromUri(uri)
                
                // Create imports directory if it doesn't exist
                val externalFilesDir = getExternalFilesDir(null)
                val importsDir = File(externalFilesDir, IMPORTS_DIR)
                if (!importsDir.exists()) {
                    importsDir.mkdirs()
                }
                
                // Copy file to imports directory
                val outputFile = File(importsDir, fileName)
                val inputStream = contentResolver.openInputStream(uri)
                val outputStream = FileOutputStream(outputFile)
                
                inputStream?.copyTo(outputStream)
                inputStream?.close()
                outputStream.close()
                
                // Send success broadcast
                val successIntent = Intent("com.quick.browser.MODEL_IMPORT_SUCCESS").apply {
                    `package` = packageName
                }
                sendBroadcast(successIntent)
                Logger.d(TAG, "Model imported successfully: $fileName")
            } catch (e: Exception) {
                Logger.e(TAG, "Exception during model import", e)
                val errorIntent = Intent("com.quick.browser.MODEL_IMPORT_ERROR").apply {
                    putExtra("ERROR_MESSAGE", e.message ?: "Unknown error during model import")
                    `package` = packageName
                }
                sendBroadcast(errorIntent)
            }
        }
    }
    
    private fun getFileNameFromUri(uri: Uri): String? {
        return try {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        it.getString(nameIndex)
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Logger.e(TAG, "Error getting file name from URI", e)
            null
        }
    }
    
    private fun getFileSizeFromUri(uri: Uri): Long {
        return try {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (sizeIndex != -1) {
                        it.getLong(sizeIndex)
                    } else {
                        0L
                    }
                } else {
                    0L
                }
            } ?: 0L
        } catch (e: Exception) {
            Logger.e(TAG, "Error getting file size from URI", e)
            0L
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(importReceiver)
        } catch (e: Exception) {
            Logger.e(TAG, "Error unregistering receiver", e)
        }
        Logger.d(TAG, "LlmModelImportService destroyed")
    }
}