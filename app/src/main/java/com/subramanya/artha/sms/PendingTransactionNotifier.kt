package com.subramanya.artha.sms

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.subramanya.artha.MainActivity
import com.subramanya.artha.R

/** Owns the single ongoing "N transactions to review" notification — updated in place
 *  (same ID) rather than stacking one notification per detected SMS. */
object PendingTransactionNotifier {

    private const val CHANNEL_ID = "pending_sms_review"
    private const val NOTIFICATION_ID = 1001
    const val EXTRA_OPEN_REVIEW = "com.subramanya.artha.extra.OPEN_REVIEW"

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.review_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun update(context: Context, pendingCount: Int) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (pendingCount <= 0) {
            manager.cancel(NOTIFICATION_ID)
            return
        }
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_REVIEW, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(
                context.resources.getQuantityString(R.plurals.review_notification_title, pendingCount, pendingCount),
            )
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }
}
