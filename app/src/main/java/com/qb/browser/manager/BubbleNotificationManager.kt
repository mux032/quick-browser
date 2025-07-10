package com.qb.browser.manager

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.qb.browser.R
import com.qb.browser.ui.main.MainActivity

/**
 * BubbleNotificationManager handles the creation and management of notifications
 * for the bubble service. It ensures the service can run in the foreground and
 * provides user interaction points through notifications.
 */
class BubbleNotificationManager(private val context: Context) {
    private val notificationManager: NotificationManager = 
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    companion object {
        const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "bubble_browser_channel"
        private const val CHANNEL_NAME = "Bubble Browser Service"
        private const val CHANNEL_DESCRIPTION = "Keeps QB Browser bubbles running"
    }

    init {
        createNotificationChannel()
    }

    /**
     * Creates the notification channel for Android O and above
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Creates a notification for the foreground service
     * @return Notification object
     */
    fun createNotification(): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_web_page)
            .setContentTitle("QB Browser Active")
            .setContentText("Browser bubbles are active")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .build()
    }

    /**
     * Updates the active notification with new content
     * @param title New notification title
     * @param content New notification content
     */
    fun updateNotification(title: String, content: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_web_page)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}