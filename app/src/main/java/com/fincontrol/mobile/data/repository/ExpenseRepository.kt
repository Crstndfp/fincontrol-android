package com.fincontrol.mobile.data.repository

import android.content.Context
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.fincontrol.mobile.data.local.FinControlDatabase
import com.fincontrol.mobile.data.local.LocalExpenseDao
import com.fincontrol.mobile.data.local.LocalExpenseEntity
import com.fincontrol.mobile.data.sync.ExpenseSyncWorker
import kotlinx.coroutines.flow.Flow

/**
 * Offline-First Repository for FinControl Android.
 * Guarantees sub-second local writes to Room SQLite (5-second UX rule)
 * and dispatches background sync via WorkManager to fincontrol-web API.
 */
class ExpenseRepository(
    private val expenseDao: LocalExpenseDao,
    private val context: Context
) {

    fun observeRecentExpenses(): Flow<List<LocalExpenseEntity>> {
        return expenseDao.observeRecentFiveExpenses()
    }

    suspend fun saveExpenseLocally(expense: LocalExpenseEntity) {
        expenseDao.insertExpense(expense)
        scheduleBackgroundSync()
    }

    suspend fun updateExpense(expense: LocalExpenseEntity) {
        expenseDao.insertExpense(expense)
        scheduleBackgroundSync()
    }

    suspend fun deleteExpense(id: String) {
        expenseDao.deleteExpenseById(id)
    }

    private fun scheduleBackgroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWork = OneTimeWorkRequestBuilder<ExpenseSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(syncWork)
    }
}
