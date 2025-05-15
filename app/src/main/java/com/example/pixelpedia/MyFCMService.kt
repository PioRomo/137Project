package com.example.pixelpedia

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat

class MyFCMService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        showNotification(message.notification?.title, message.notification?.body)
    }

    private fun showNotification(title:String?, message:String?) {
        val notification = NotificationCompat.Builder(applicationContext, CHANNELZ_ID)
            .setSmallIcon(R.drawable.red_minus_icon)
            .setContentTitle(title)
            .setContentText(message)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(100, notification)
    }
}