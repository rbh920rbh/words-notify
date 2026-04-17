package com.example.wordsnotify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private lateinit var wordCountText: TextView

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            scheduleNotificationIn3Seconds()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        wordCountText = findViewById(R.id.wordCountText)
        createNotificationChannel()
        setupImportButton()
        setupShowSchemaButton()
        refreshWordCount()
    }

    override fun onStart() {
        super.onStart()
        ensurePermissionAndNotify()
    }

    private fun ensurePermissionAndNotify() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                scheduleNotificationIn3Seconds()
            } else {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            scheduleNotificationIn3Seconds()
        }
    }

    private fun scheduleNotificationIn3Seconds() {
        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(3, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniqueWork(
            NOTIFICATION_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Words Notify"
            val descriptionText = "Word notification channel"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun setupImportButton() {
        val importButton: Button = findViewById(R.id.importWordBookButton)
        val importStatusText: TextView = findViewById(R.id.importStatusText)

        importButton.setOnClickListener {
            importButton.isEnabled = false
            importStatusText.text = getString(R.string.import_status_running)

            Thread {
                val result = runCatching {
                    WordBookImporter(this).importFromAsset("toefl.json")
                }

                runOnUiThread {
                    importButton.isEnabled = true
                    result.onSuccess {
                        importStatusText.text = getString(
                            R.string.import_status_success,
                            it.importedCount
                        )
                        refreshWordCount()
                    }.onFailure { throwable ->
                        importStatusText.text = getString(
                            R.string.import_status_failed,
                            throwable.message ?: "unknown error"
                        )
                    }
                }
            }.start()
        }
    }

    private fun setupShowSchemaButton() {
        val showSchemaButton: Button = findViewById(R.id.showSchemaButton)
        val importStatusText: TextView = findViewById(R.id.importStatusText)

        showSchemaButton.setOnClickListener {
            showSchemaButton.isEnabled = false
            importStatusText.text = getString(R.string.schema_status_running)

            Thread {
                val result = runCatching {
                    WordBookDatabaseHelper(this).use { helper ->
                        helper.getTableSchemaSummary()
                    }
                }

                runOnUiThread {
                    showSchemaButton.isEnabled = true
                    result.onSuccess { schema ->
                        importStatusText.text = getString(R.string.schema_status_result, schema)
                    }.onFailure { throwable ->
                        importStatusText.text = getString(
                            R.string.schema_status_failed,
                            throwable.message ?: "unknown error"
                        )
                    }
                }
            }.start()
        }
    }

    private fun refreshWordCount() {
        Thread {
            val count = runCatching {
                WordBookDatabaseHelper(this).use { helper ->
                    helper.getWordCount()
                }
            }.getOrDefault(0)

            runOnUiThread {
                wordCountText.text = getString(R.string.word_count_label, count)
            }
        }.start()
    }

    companion object {
        const val CHANNEL_ID = "words_notify_channel"
        const val NOTIFICATION_ID = 10001
        private const val NOTIFICATION_WORK_NAME = "notify_after_foreground"
    }
}
