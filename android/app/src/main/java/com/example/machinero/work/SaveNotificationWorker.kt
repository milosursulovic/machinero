package com.example.machinero.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.machinero.data.AppDatabase
import com.example.machinero.data.NotificationEntity

class SaveNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString("title") ?: return Result.failure()
        val body = inputData.getString("body") ?: ""

        val entity = NotificationEntity(
            title = title,
            body = body,
            orderId = inputData.getString("orderId"),
            status = inputData.getString("status"),
            customer = inputData.getString("customer"),
            deliveryDateIso = inputData.getString("deliveryDate"),
            type = inputData.getString("type"),
            receivedAtMillis = System.currentTimeMillis()
        )

        val dao = AppDatabase.getInstance(applicationContext).notificationDao()
        dao.insert(entity)
        return Result.success()
    }
}
