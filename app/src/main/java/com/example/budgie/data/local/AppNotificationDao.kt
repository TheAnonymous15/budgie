package com.example.budgie.data.local

import androidx.room.*
import com.example.budgie.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppNotificationDao {

    // ═══════════════════════════════════════════════════════════════════════════════
    // MAIN NOTIFICATION QUERIES
    // ═══════════════════════════════════════════════════════════════════════════════

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotification): Long

    @Update
    suspend fun updateNotification(notification: AppNotification)

    @Delete
    suspend fun deleteNotification(notification: AppNotification)

    @Query("SELECT * FROM app_notifications WHERE id = :id")
    suspend fun getNotificationById(id: Long): AppNotification?

    @Query("SELECT * FROM app_notifications WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getAllActiveNotifications(): Flow<List<AppNotification>>

    @Query("SELECT * FROM app_notifications WHERE isArchived = 0 ORDER BY createdAt DESC")
    suspend fun getAllActiveNotificationsOnce(): List<AppNotification>

    @Query("SELECT * FROM app_notifications WHERE status = :status AND isArchived = 0 ORDER BY createdAt DESC")
    fun getNotificationsByStatus(status: NotificationStatus): Flow<List<AppNotification>>

    @Query("SELECT * FROM app_notifications WHERE category = :category AND isArchived = 0 ORDER BY createdAt DESC")
    fun getNotificationsByCategory(category: NotificationCategory): Flow<List<AppNotification>>

    @Query("SELECT * FROM app_notifications WHERE status = 'UNREAD' AND isArchived = 0 ORDER BY priority DESC, createdAt DESC")
    fun getUnreadNotifications(): Flow<List<AppNotification>>

    @Query("SELECT * FROM app_notifications WHERE status = 'UNREAD' AND isArchived = 0 ORDER BY priority DESC, createdAt DESC")
    suspend fun getUnreadNotificationsOnce(): List<AppNotification>

    @Query("SELECT COUNT(*) FROM app_notifications WHERE status = 'UNREAD' AND isArchived = 0")
    fun getUnreadCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM app_notifications WHERE status = 'UNREAD' AND isArchived = 0")
    suspend fun getUnreadCountOnce(): Int

    @Query("SELECT COUNT(*) FROM app_notifications WHERE status = 'UNREAD' AND isArchived = 0 AND category = :category")
    fun getUnreadCountByCategory(category: NotificationCategory): Flow<Int>

    @Query("UPDATE app_notifications SET status = 'READ', readAt = :readAt WHERE id = :id")
    suspend fun markAsRead(id: Long, readAt: Long = System.currentTimeMillis())

    @Query("UPDATE app_notifications SET status = 'READ', readAt = :readAt WHERE status = 'UNREAD'")
    suspend fun markAllAsRead(readAt: Long = System.currentTimeMillis())

    @Query("UPDATE app_notifications SET status = 'READ', readAt = :readAt WHERE category = :category AND status = 'UNREAD'")
    suspend fun markCategoryAsRead(category: NotificationCategory, readAt: Long = System.currentTimeMillis())

    @Query("UPDATE app_notifications SET status = 'DISMISSED' WHERE id = :id")
    suspend fun dismissNotification(id: Long)

    @Query("UPDATE app_notifications SET isArchived = 1 WHERE id = :id")
    suspend fun archiveNotification(id: Long)

    @Query("DELETE FROM app_notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM app_notifications WHERE createdAt < :before AND isArchived = 1")
    suspend fun deleteOldArchivedNotifications(before: Long)

    @Query("DELETE FROM app_notifications WHERE expiresAt IS NOT NULL AND expiresAt < :now")
    suspend fun deleteExpiredNotifications(now: Long = System.currentTimeMillis())

    @Query("DELETE FROM app_notifications WHERE status = 'READ'")
    suspend fun deleteAllRead()

    @Query("DELETE FROM app_notifications")
    suspend fun deleteAll()

    @Query("""
        SELECT * FROM app_notifications 
        WHERE (priority = 'URGENT' OR priority = 'HIGH')
        AND status = 'UNREAD' AND isArchived = 0
        ORDER BY priority DESC, createdAt DESC
    """)
    fun getHighPriorityNotifications(): Flow<List<AppNotification>>

    @Query("SELECT * FROM app_notifications WHERE createdAt >= :since AND isArchived = 0 ORDER BY createdAt DESC")
    fun getRecentNotifications(since: Long): Flow<List<AppNotification>>

    @Query("SELECT * FROM app_notifications WHERE groupId = :groupId ORDER BY createdAt DESC")
    fun getNotificationsByGroup(groupId: String): Flow<List<AppNotification>>

    // ═══════════════════════════════════════════════════════════════════════════════
    // BILL NOTIFICATION QUERIES
    // ═══════════════════════════════════════════════════════════════════════════════

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillNotification(detail: BillNotificationDetail): Long

    @Query("SELECT * FROM bill_notifications WHERE notificationId = :notificationId")
    suspend fun getBillNotificationDetail(notificationId: Long): BillNotificationDetail?

    @Query("SELECT * FROM bill_notifications WHERE billId = :billId ORDER BY id DESC")
    fun getBillNotificationsByBillId(billId: Long): Flow<List<BillNotificationDetail>>

    @Query("""
        SELECT bn.* FROM bill_notifications bn
        INNER JOIN app_notifications an ON bn.notificationId = an.id
        WHERE bn.type = :type AND an.isArchived = 0
        ORDER BY an.createdAt DESC
    """)
    fun getBillNotificationsByType(type: BillNotificationType): Flow<List<BillNotificationDetail>>

    // ═══════════════════════════════════════════════════════════════════════════════
    // BUDGET NOTIFICATION QUERIES
    // ═══════════════════════════════════════════════════════════════════════════════

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetNotification(detail: BudgetNotificationDetail): Long

    @Query("SELECT * FROM budget_notifications WHERE notificationId = :notificationId")
    suspend fun getBudgetNotificationDetail(notificationId: Long): BudgetNotificationDetail?

    @Query("SELECT * FROM budget_notifications WHERE budgetId = :budgetId ORDER BY id DESC")
    fun getBudgetNotificationsByBudgetId(budgetId: Long): Flow<List<BudgetNotificationDetail>>

    @Query("""
        SELECT bn.* FROM budget_notifications bn
        INNER JOIN app_notifications an ON bn.notificationId = an.id
        WHERE bn.percentageUsed >= :threshold AND an.isArchived = 0
        ORDER BY bn.percentageUsed DESC
    """)
    fun getBudgetAlertsAboveThreshold(threshold: Double): Flow<List<BudgetNotificationDetail>>

    // ═══════════════════════════════════════════════════════════════════════════════
    // GOAL NOTIFICATION QUERIES
    // ═══════════════════════════════════════════════════════════════════════════════

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalNotification(detail: GoalNotificationDetail): Long

    @Query("SELECT * FROM goal_notifications WHERE notificationId = :notificationId")
    suspend fun getGoalNotificationDetail(notificationId: Long): GoalNotificationDetail?

    @Query("SELECT * FROM goal_notifications WHERE goalId = :goalId ORDER BY id DESC")
    fun getGoalNotificationsByGoalId(goalId: Long): Flow<List<GoalNotificationDetail>>

    @Query("""
        SELECT gn.* FROM goal_notifications gn
        INNER JOIN app_notifications an ON gn.notificationId = an.id
        WHERE gn.type = 'GOAL_ACHIEVED' AND an.isArchived = 0
        ORDER BY an.createdAt DESC
    """)
    fun getGoalAchievements(): Flow<List<GoalNotificationDetail>>

    // ═══════════════════════════════════════════════════════════════════════════════
    // LOAN NOTIFICATION QUERIES
    // ═══════════════════════════════════════════════════════════════════════════════

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLoanNotification(detail: LoanNotificationDetail): Long

    @Query("SELECT * FROM loan_notifications WHERE notificationId = :notificationId")
    suspend fun getLoanNotificationDetail(notificationId: Long): LoanNotificationDetail?

    @Query("SELECT * FROM loan_notifications WHERE loanId = :loanId ORDER BY id DESC")
    fun getLoanNotificationsByLoanId(loanId: Long): Flow<List<LoanNotificationDetail>>

    @Query("""
        SELECT ln.* FROM loan_notifications ln
        INNER JOIN app_notifications an ON ln.notificationId = an.id
        WHERE ln.type IN ('PAYMENT_DUE', 'PAYMENT_DUE_TODAY', 'PAYMENT_OVERDUE') AND an.isArchived = 0
        ORDER BY ln.nextPaymentDate ASC
    """)
    fun getUpcomingLoanPayments(): Flow<List<LoanNotificationDetail>>

    // ═══════════════════════════════════════════════════════════════════════════════
    // EXPENSE NOTIFICATION QUERIES
    // ═══════════════════════════════════════════════════════════════════════════════

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseNotification(detail: ExpenseNotificationDetail): Long

    @Query("SELECT * FROM expense_notifications WHERE notificationId = :notificationId")
    suspend fun getExpenseNotificationDetail(notificationId: Long): ExpenseNotificationDetail?

    @Query("SELECT * FROM expense_notifications WHERE expenseId = :expenseId ORDER BY id DESC")
    fun getExpenseNotificationsByExpenseId(expenseId: Long): Flow<List<ExpenseNotificationDetail>>

    @Query("""
        SELECT en.* FROM expense_notifications en
        INNER JOIN app_notifications an ON en.notificationId = an.id
        WHERE en.type = 'ANOMALY_DETECTED' AND an.isArchived = 0
        ORDER BY an.createdAt DESC
    """)
    fun getExpenseAnomalies(): Flow<List<ExpenseNotificationDetail>>

    // ═══════════════════════════════════════════════════════════════════════════════
    // INCOME NOTIFICATION QUERIES
    // ═══════════════════════════════════════════════════════════════════════════════

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomeNotification(detail: IncomeNotificationDetail): Long

    @Query("SELECT * FROM income_notifications WHERE notificationId = :notificationId")
    suspend fun getIncomeNotificationDetail(notificationId: Long): IncomeNotificationDetail?

    @Query("SELECT * FROM income_notifications WHERE incomeId = :incomeId ORDER BY id DESC")
    fun getIncomeNotificationsByIncomeId(incomeId: Long): Flow<List<IncomeNotificationDetail>>

    // ═══════════════════════════════════════════════════════════════════════════════
    // SECURITY NOTIFICATION QUERIES
    // ═══════════════════════════════════════════════════════════════════════════════

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSecurityNotification(detail: SecurityNotificationDetail): Long

    @Query("SELECT * FROM security_notifications WHERE notificationId = :notificationId")
    suspend fun getSecurityNotificationDetail(notificationId: Long): SecurityNotificationDetail?

    @Query("""
        SELECT sn.* FROM security_notifications sn
        INNER JOIN app_notifications an ON sn.notificationId = an.id
        WHERE an.isArchived = 0
        ORDER BY sn.timestamp DESC
    """)
    fun getAllSecurityNotifications(): Flow<List<SecurityNotificationDetail>>

    @Query("""
        SELECT sn.* FROM security_notifications sn
        INNER JOIN app_notifications an ON sn.notificationId = an.id
        WHERE (sn.securityLevel = 'CRITICAL' OR sn.actionRequired = 1) AND an.isArchived = 0
        ORDER BY sn.timestamp DESC
    """)
    fun getCriticalSecurityAlerts(): Flow<List<SecurityNotificationDetail>>

    // ═══════════════════════════════════════════════════════════════════════════════
    // SYSTEM NOTIFICATION QUERIES
    // ═══════════════════════════════════════════════════════════════════════════════

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSystemNotification(detail: SystemNotificationDetail): Long

    @Query("SELECT * FROM system_notifications WHERE notificationId = :notificationId")
    suspend fun getSystemNotificationDetail(notificationId: Long): SystemNotificationDetail?

    @Query("""
        SELECT sn.* FROM system_notifications sn
        INNER JOIN app_notifications an ON sn.notificationId = an.id
        WHERE an.isArchived = 0
        ORDER BY an.createdAt DESC
    """)
    fun getAllSystemNotifications(): Flow<List<SystemNotificationDetail>>

    // ═══════════════════════════════════════════════════════════════════════════════
    // SHOPPING NOTIFICATION QUERIES
    // ═══════════════════════════════════════════════════════════════════════════════

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingNotification(detail: ShoppingNotificationDetail): Long

    @Query("SELECT * FROM shopping_notifications WHERE notificationId = :notificationId")
    suspend fun getShoppingNotificationDetail(notificationId: Long): ShoppingNotificationDetail?

    @Query("SELECT * FROM shopping_notifications WHERE listId = :listId ORDER BY id DESC")
    fun getShoppingNotificationsByListId(listId: Long): Flow<List<ShoppingNotificationDetail>>

    // ═══════════════════════════════════════════════════════════════════════════════
    // INVESTMENT NOTIFICATION QUERIES
    // ═══════════════════════════════════════════════════════════════════════════════

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestmentNotification(detail: InvestmentNotificationDetail): Long

    @Query("SELECT * FROM investment_notifications WHERE notificationId = :notificationId")
    suspend fun getInvestmentNotificationDetail(notificationId: Long): InvestmentNotificationDetail?

    @Query("""
        SELECT inv.* FROM investment_notifications inv
        INNER JOIN app_notifications an ON inv.notificationId = an.id
        WHERE an.isArchived = 0
        ORDER BY an.createdAt DESC
    """)
    fun getAllInvestmentNotifications(): Flow<List<InvestmentNotificationDetail>>

    // ═══════════════════════════════════════════════════════════════════════════════
    // INSIGHT NOTIFICATION QUERIES
    // ═══════════════════════════════════════════════════════════════════════════════

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsightNotification(detail: InsightNotificationDetail): Long

    @Query("SELECT * FROM insight_notifications WHERE notificationId = :notificationId")
    suspend fun getInsightNotificationDetail(notificationId: Long): InsightNotificationDetail?

    @Query("""
        SELECT ins.* FROM insight_notifications ins
        INNER JOIN app_notifications an ON ins.notificationId = an.id
        WHERE an.isArchived = 0
        ORDER BY an.createdAt DESC
        LIMIT :limit
    """)
    fun getRecentInsights(limit: Int = 10): Flow<List<InsightNotificationDetail>>

    @Query("""
        SELECT ins.* FROM insight_notifications ins
        INNER JOIN app_notifications an ON ins.notificationId = an.id
        WHERE ins.type = :type AND an.isArchived = 0
        ORDER BY an.createdAt DESC
    """)
    fun getInsightsByType(type: InsightNotificationType): Flow<List<InsightNotificationDetail>>

    // ═══════════════════════════════════════════════════════════════════════════════
    // AGGREGATED QUERIES
    // ═══════════════════════════════════════════════════════════════════════════════

    @Query("""
        SELECT category, COUNT(*) as count 
        FROM app_notifications 
        WHERE status = 'UNREAD' AND isArchived = 0
        GROUP BY category
    """)
    fun getUnreadCountByAllCategories(): Flow<List<CategoryNotificationCount>>

    @Query("""
        SELECT * FROM app_notifications 
        WHERE createdAt >= :todayStart AND createdAt <= :todayEnd
        AND isArchived = 0
        ORDER BY createdAt DESC
    """)
    fun getTodaysNotifications(todayStart: Long, todayEnd: Long): Flow<List<AppNotification>>
}

