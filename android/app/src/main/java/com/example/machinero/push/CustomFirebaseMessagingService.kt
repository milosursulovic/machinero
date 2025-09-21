package com.example.machinero.push

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import com.example.machinero.ui.OrderDetailActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class CustomFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onMessageReceived(msg: RemoteMessage) {
        super.onMessageReceived(msg)

        val d = msg.data

        val title = d["_title"] ?: d["title"] ?: msg.notification?.title ?: "Obaveštenje"
        val body  = d["_body"]  ?: d["body"]  ?: msg.notification?.body  ?: ""

        val orderId      = d["orderId"]
        val status       = d["status"]
        val customer     = d["customer"]
        val deliveryDate = d["deliveryDate"]
        val type         = d["type"]

        val detailIntent = Intent(this, OrderDetailActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("_title", title)
            putExtra("_body", body)
            orderId?.let      { putExtra("orderId", it) }
            status?.let       { putExtra("status", it) }
            customer?.let     { putExtra("customer", it) }
            deliveryDate?.let { putExtra("deliveryDate", it) }
            type?.let         { putExtra("type", it) }
        }

        val stackBuilder = TaskStackBuilder.create(this).apply {
            addNextIntentWithParentStack(detailIntent)
        }

        val reqCode = (orderId ?: Random.nextInt().toString()).hashCode()

        val pi = stackBuilder.getPendingIntent(
            reqCode,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        NotificationUtil.ensureChannel(this)

        val notif = NotificationCompat.Builder(this, NotificationUtil.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        NotificationManagerCompat.from(this).notify(Random.nextInt(), notif)
    }
}
