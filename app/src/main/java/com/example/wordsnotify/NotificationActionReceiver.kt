package com.example.wordsnotify

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class NotificationActionReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val notificationId = intent?.getIntExtra(EXTRA_NOTIFICATION_ID, MainActivity.NOTIFICATION_ID)
            ?: MainActivity.NOTIFICATION_ID

        val notificationManager =
            ContextCompat.getSystemService(context, NotificationManager::class.java)
        notificationManager?.cancel(notificationId)
    }

    companion object {
        const val ACTION_CANCEL = "com.example.wordsnotify.action.CANCEL"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
