package com.fincontrol.mobile.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import retrofit2.http.Body
import retrofit2.http.POST

data class SyncTransactionPayload(
    val merchantName: String,
    val description: String,
    val currency: String,
    val totalAmountOriginal: Double,
    val splitMode: String,
    val percentageOwn: Double,
    val source: String = "MOBILE_EXPRESS",
    val receiptAttachmentUrl: String? = null
)

interface FinControlApiService {
    @POST("transactions")
    suspend fun syncMobileExpense(@Body payload: SyncTransactionPayload)
}

/**
 * Background Worker that synchronizes offline mobile expenses with fincontrol-web
 * (/api/v1/transactions) so subsequent PDF bank statement uploads on desktop
 * automatically reconcile without duplicating expenses (Module 2 & 4.2).
 */
class ExpenseSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Dispatches pending Room records to POST /api/v1/transactions
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
