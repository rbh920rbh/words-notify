package com.example.wordsnotify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        createNotificationChannel(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return Result.success()
        }

        val openAppIntent = Intent(applicationContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val cancelIntent = Intent(applicationContext, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_CANCEL
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, MainActivity.NOTIFICATION_ID)
        }

        val openAppPendingIntent = PendingIntent.getActivity(
            applicationContext,
            2001,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val cancelPendingIntent = PendingIntent.getBroadcast(
            applicationContext,
            2002,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val titleText = "significant [sɪɡ'nɪfɪkənt]"
        val bodyText = """
            Please inform us if there are any significant changes in your plans.
            
            adj: 重大的；有效的；有意义的；值得注意的；意味深长的
            
            你们的计划如有重大变动，请通知我们。
        """.trimIndent()
        val notification = NotificationCompat.Builder(applicationContext, MainActivity.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(titleText)
            .setContentText(bodyText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(false)
            .addAction(0, "确定", openAppPendingIntent)
            .addAction(0, "取消", cancelPendingIntent)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(MainActivity.NOTIFICATION_ID, notification)
        return Result.success()
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                MainActivity.CHANNEL_ID,
                "Words Notify",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Word notification channel"
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}
