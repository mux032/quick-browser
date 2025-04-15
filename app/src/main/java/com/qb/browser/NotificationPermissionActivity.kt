package com.qb.browser

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.qb.browser.service.BubbleService

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class NotificationPermissionActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make activity transparent
        setTheme(android.R.style.Theme_Translucent_NoTitleBar)

        // Check if the permission is already granted
        if (ContextCompat.checkSelfPermission(
                this, 
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, finish the activity
            finishAndStartService()
        } else {
            // Request the POST_NOTIFICATIONS permission
            ActivityCompat.requestPermissions(
                this, 
                arrayOf(Manifest.permission.POST_NOTIFICATIONS), 
                REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, 
        permissions: Array<out String>, 
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE) {
            // Handle the result of the permission request
            if (grantResults.isNotEmpty() && 
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                finishAndStartService()
            } else {
                // Show a toast if permission was denied
                Toast.makeText(
                    this,
                    "Notification permission is required for optimal functionality",
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }
    
    private fun finishAndStartService() {
        // Try to start/restart the bubble service
        val serviceIntent = Intent(this, BubbleService::class.java).apply {
            action = BubbleService.ACTION_START_BUBBLE
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        finish()
    }

    companion object {
        private const val REQUEST_CODE = 1001
    }
}