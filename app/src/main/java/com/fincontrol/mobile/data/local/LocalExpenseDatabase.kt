package com.fincontrol.mobile.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Offline-First Local Expense Entity (Room SQLite)
 * RN-01: Stores total, personal, and receivable amounts independently.
 * RN-03: Zero tax ID (NIT) or invoice number constraints.
 */
@Entity(tableName = "local_expenses")
data class LocalExpenseEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val merchantName: String,
    val categoryName: String,
    val currency: String, // "GTQ" or "USD"
    val totalAmountOriginal: Double,
    val totalAmountGtq: Double,
    val isSplitExpense: Boolean,
    val splitPercentage: Double,
    val personalAmountGtq: Double,
    val receivableAmountGtq: Double,
    val timestampEpochMillis: Long = System.currentTimeMillis(),
    val attachmentUri: String? = null,
    val syncStatus: String = "PENDING_SYNC" // "PENDING_SYNC", "SYNCED_AWAITING_PDF_RECONCILE"
)

@Dao
interface LocalExpenseDao {
    @Query("SELECT * FROM local_expenses ORDER BY timestampEpochMillis DESC LIMIT 5")
    fun observeRecentFiveExpenses(): Flow<List<LocalExpenseEntity>>

    @Query("SELECT * FROM local_expenses WHERE syncStatus = 'PENDING_SYNC'")
    suspend fun getPendingSyncExpenses(): List<LocalExpenseEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: LocalExpenseEntity)

    @Query("DELETE FROM local_expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: String)

    @Query("UPDATE local_expenses SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: String)
}

@Database(entities = [LocalExpenseEntity::class], version = 1, exportSchema = false)
abstract class FinControlDatabase : RoomDatabase() {
    abstract fun expenseDao(): LocalExpenseDao
}
