package com.example.machinero.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val body: String,
    val orderId: String?,
    val status: String?,
    val customer: String?,
    val deliveryDateIso: String?,
    val type: String?,
    val receivedAtMillis: Long
)
