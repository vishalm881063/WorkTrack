package com.worktrack.app

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object Notifier {
    private const val CH_REMINDER = "reminder"
    private const val CH_DONE = "done"

    fun ensureChannels(c: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = c.getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CH_REMINDER, "Daily login reminder", NotificationManager.IMPORTANCE_HIGH)
            )
            nm.createNotificationChannel(
                NotificationChannel(CH_DONE, "Daily target completed", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    fun enabled(c: Context): Boolean = NotificationManagerCompat.from(c).areNotificationsEnabled()

    @SuppressLint("MissingPermission")
    private fun show(c: Context, id: Int, channel: String, title: String, text: String) {
        ensureChannels(c)
        if (!enabled(c)) return
        val open = PendingIntent.getActivity(
            c, 0,
            Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(c, channel)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        try {
            NotificationManagerCompat.from(c).notify(id, n)
        } catch (e: SecurityException) {
        }
    }

    fun reminder(c: Context) =
        show(c, 101, CH_REMINDER, "Good morning! Time to log in", "Open WorkTrack and tap Login to start tracking your day.")

    fun completed(c: Context, targetMs: Long) {
        val m = targetMs / 60_000L
        show(c, 102, CH_DONE, "Daily target completed \uD83C\uDF89", "You've worked ${m / 60}h ${m % 60}m today. Great job!")
    }
}
