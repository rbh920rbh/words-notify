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

        if (intent?.action == ACTION_OPEN_APP) {
            val launchIntent = Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            context.startActivity(launchIntent)
        }
    }

    companion object {
        const val ACTION_OPEN_APP = "com.example.wordsnotify.action.OPEN_APP"
        const val ACTION_CANCEL = "com.example.wordsnotify.action.CANCEL"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
    }
}
