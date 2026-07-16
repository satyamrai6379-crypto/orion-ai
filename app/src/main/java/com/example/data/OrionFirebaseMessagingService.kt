package com.example.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class OrionFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("OrionFCM", "New FCM registration token received: $token")
        // Store or transmit token to backend if needed in the future
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d("OrionFCM", "Message received from: ${remoteMessage.from}")

        // Check if message contains notification payload
        remoteMessage.notification?.let {
            val title = it.title ?: "Orion AI Constellation"
            val body = it.body ?: "Your celestial companion has an update."
            showSystemNotification(title, body)
        } ?: run {
            // Check if message contains data payload
            val title = remoteMessage.data["title"] ?: "Orion AI Update"
            val body = remoteMessage.data["body"] ?: "A new starlight signal is ready."
            showSystemNotification(title, body)
        }
    }

    private fun showSystemNotification(title: String, body: String) {
        val channelId = "orion_signals_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create the NotificationChannel for API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Orion Celestial Signals",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority notifications and updates from the Orion AI Constellation"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use default foreground vector
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
