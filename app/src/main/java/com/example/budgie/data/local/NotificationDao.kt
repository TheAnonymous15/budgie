package com.example.budgie.data.local

import androidx.room.*
import com.example.budgie.data.model.BudgieNotification
import com.example.budgie.data.model.NotificationPriority
import com.example.budgie.data.model.NotificationType
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    // Get all notifications (newest first)
    @Query("SELECT * FROM notifications WHERE isArchived = 0 ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<BudgieNotification>>

    // Get all notifications once (for one-time reads)
    @Query("SELECT * FROM notifications WHERE isArchived = 0 ORDER BY timestamp DESC")
    suspend fun getAllNotificationsOnce(): List<BudgieNotification>

    // Get unread notifications
    @Query("SELECT * FROM notifications WHERE isRead = 0 AND isArchived = 0 ORDER BY timestamp DESC")
    fun getUnreadNotifications(): Flow<List<BudgieNotification>>

    // Get unread count
    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0 AND isArchived = 0")
    fun getUnreadCount(): Flow<Int>

    // Get unread count once
    @Query("SELECT COUNT(*) FROM notifications WHERE isRead = 0 AND isArchived = 0")
    suspend fun getUnreadCountOnce(): Int

    // Get notifications by type
    @Query("SELECT * FROM notifications WHERE type = :type AND isArchived = 0 ORDER BY timestamp DESC")
    fun getNotificationsByType(type: NotificationType): Flow<List<BudgieNotification>>

    // Get notifications by priority
    @Query("SELECT * FROM notifications WHERE priority = :priority AND isArchived = 0 ORDER BY timestamp DESC")
    fun getNotificationsByPriority(priority: NotificationPriority): Flow<List<BudgieNotification>>

    // Get high priority unread notifications
    @Query("SELECT * FROM notifications WHERE priority >= :minPriority AND isRead = 0 AND isArchived = 0 ORDER BY timestamp DESC")
    fun getHighPriorityUnread(minPriority: NotificationPriority = NotificationPriority.HIGH): Flow<List<BudgieNotification>>

    // Get recent notifications (last 24 hours)
    @Query("SELECT * FROM notifications WHERE timestamp >= :since AND isArchived = 0 ORDER BY timestamp DESC")
    fun getRecentNotifications(since: Long): Flow<List<BudgieNotification>>

    // Get archived notifications
    @Query("SELECT * FROM notifications WHERE isArchived = 1 ORDER BY timestamp DESC")
    fun getArchivedNotifications(): Flow<List<BudgieNotification>>

    // Get notification by ID
    @Query("SELECT * FROM notifications WHERE id = :id")
    suspend fun getNotificationById(id: Long): BudgieNotification?

    // Insert notification
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: BudgieNotification): Long

    // Insert multiple notifications
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<BudgieNotification>)

    // Update notification
    @Update
    suspend fun updateNotification(notification: BudgieNotification)

    // Mark as read
    @Query("UPDATE notifications SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: Long)

    // Mark all as read
    @Query("UPDATE notifications SET isRead = 1 WHERE isRead = 0")
    suspend fun markAllAsRead()

    // Archive notification
    @Query("UPDATE notifications SET isArchived = 1 WHERE id = :id")
    suspend fun archiveNotification(id: Long)

    // Delete notification
    @Delete
    suspend fun deleteNotification(notification: BudgieNotification)

    // Delete by ID
    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    // Delete all read notifications
    @Query("DELETE FROM notifications WHERE isRead = 1")
    suspend fun deleteAllRead()

    // Delete expired notifications
    @Query("DELETE FROM notifications WHERE expiresAt IS NOT NULL AND expiresAt < :currentTime")
    suspend fun deleteExpired(currentTime: Long = System.currentTimeMillis())

    // Delete all notifications
    @Query("DELETE FROM notifications")
    suspend fun deleteAll()

    // Count notifications by type
    @Query("SELECT COUNT(*) FROM notifications WHERE type = :type AND isArchived = 0")
    suspend fun countByType(type: NotificationType): Int
}

