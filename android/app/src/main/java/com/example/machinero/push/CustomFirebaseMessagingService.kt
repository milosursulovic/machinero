package com.example.machinero.push

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.example.machinero.R
import com.example.machinero.data.AppDatabase
import com.example.machinero.data.NotificationEntity
import com.example.machinero.ui.OrderDetailActivity
import com.example.machinero.work.SaveNotificationWorker
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class CustomFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM_SVC", "New token: $token")
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
        val deliveryDate = d["deliveryDate"] // ISO string
        val type         = d["type"]

        Log.d("FCM_SVC", "onMessageReceived title='$title' data=$d")

        val entity = NotificationEntity(
            title = title,
            body = body,
            orderId = orderId,
            status = status,
            customer = customer,
            deliveryDateIso = deliveryDate,
            type = type,
            receivedAtMillis = System.currentTimeMillis()
        )

        var saved = false
        try {
            runBlocking {
                withContext(Dispatchers.IO) {
                    val dao = AppDatabase.getInstance(applicationContext).notificationDao()
                    dao.insert(entity)
                }
            }
            saved = true
            Log.d("FCM_SVC", "Room insert OK (sync)")
        } catch (t: Throwable) {
            Log.e("FCM_SVC", "Room insert FAILED (sync), enqueue WM fallback", t)
            val workData = Data.Builder()
                .putString("title", title)
                .putString("body", body)
                .putString("orderId", orderId)
                .putString("status", status)
                .putString("customer", customer)
                .putString("deliveryDate", deliveryDate)
                .putString("type", type)
                .build()
            val saveReq = OneTimeWorkRequestBuilder<SaveNotificationWorker>()
                .setInputData(workData)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(applicationContext).enqueue(saveReq)
        }

        try {
            val sp = getSharedPreferences("notif_debug", MODE_PRIVATE)
            val newCount = sp.getInt("count", 0) + 1
            sp.edit()
                .putInt("count", newCount)
                .putLong("last_ts", System.currentTimeMillis())
                .putString("last_title", title)
                .putString("last_body", body)
                .putBoolean("last_saved_sync", saved)
                .apply()
            Log.d("FCM_SVC", "SP updated: count=$newCount savedSync=$saved")
        } catch (_: Throwable) {}

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
