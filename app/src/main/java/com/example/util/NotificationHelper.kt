package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R

object NotificationHelper {

    private const val CHANNEL_ID = "welcome_channel_amit_meena"
    private const val CHANNEL_NAME = "Welcome Notifications"

    fun showWelcomeNotification(context: Context, userName: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Welcome notifications from Amit Meena"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val name = if (userName.isBlank() || userName.lowercase() == "guest user") "User" else userName
        val title = "Amit Meena"
        val message = "Welcome to Zypo AI, $name! 🙏 Hope you have a wonderful experience."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Welcome to Zypo AI, $name! 🙏 Greetings from Amit Meena. Thank you for logging in!")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)

        try {
            notificationManager.notify(1001, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
