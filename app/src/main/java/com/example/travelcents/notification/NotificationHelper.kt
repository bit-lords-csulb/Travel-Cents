package com.example.travelcents.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.travelcents.BuildConfig
import com.example.travelcents.MainActivity
import com.example.travelcents.R

class NotificationHelper(private val context: Context) {

    companion object {
        private const val TAG = "NotificationHelper"
        const val CHANNEL_RECEIVED_ID = "chat_messages_high_priority"

        var activeChatTarget: ChatNotificationTarget? = null

        private var instance: NotificationHelper? = null

        fun initialize(context: Context) {
            if (instance == null) {
                instance = NotificationHelper(context.applicationContext)
            }
        }

        fun getInstance(): NotificationHelper {
            return instance ?: throw IllegalStateException("NotificationHelper must be initialized")
        }
    }

    private fun debugLog(message: () -> String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message())
        }
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val nameRec = context.getString(R.string.channel_received_messages_name)
            val descRec = context.getString(R.string.channel_received_messages_description)
            val importanceRec = NotificationManager.IMPORTANCE_HIGH
            val channelRec = NotificationChannel(CHANNEL_RECEIVED_ID, nameRec, importanceRec).apply {
                description = descRec
                setShowBadge(true)
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
                setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI, android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            notificationManager.createNotificationChannel(channelRec)
            debugLog {
                "created or updated notification channel id=$CHANNEL_RECEIVED_ID " +
                    "importance=$importanceRec"
            }
        }
    }

    fun showChatNotification(target: ChatNotificationTarget, title: String, body: String) {
        if (target == activeChatTarget) {
            debugLog {
                "skip notification target=$target reason=activeChatTarget"
            }
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(ChatNotificationTarget.EXTRA_CHAT_TYPE, target.chatType)
            putExtra(ChatNotificationTarget.EXTRA_CHAT_ID, target.chatId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            target.notificationKey.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificationId = target.notificationKey.hashCode()

        val builder = NotificationCompat.Builder(context, CHANNEL_RECEIVED_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX) // Increased to MAX for background popups
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 500, 200, 500)) // More aggressive vibration
            .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)

        val notificationManager = NotificationManagerCompat.from(context)
        debugLog {
            "posting notification target=$target notificationId=$notificationId " +
                "title=$title notificationsEnabled=${notificationManager.areNotificationsEnabled()}"
        }

        try {
            notificationManager.notify(notificationId, builder.build())
            debugLog {
                "posted notification target=$target notificationId=$notificationId"
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Unable to post chat notification; notification permission is missing", e)
        }
    }
}
