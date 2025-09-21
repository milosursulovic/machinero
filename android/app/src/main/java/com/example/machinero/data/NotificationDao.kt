package com.example.machinero.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NotificationDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(n: NotificationEntity): Long

    @Query("SELECT COUNT(*) FROM notifications")
    suspend fun count(): Int

    @Query("SELECT * FROM notifications ORDER BY receivedAtMillis DESC")
    fun getAllLive(): LiveData<List<NotificationEntity>>

    @Query("DELETE FROM notifications")
    suspend fun clearAll()
}
