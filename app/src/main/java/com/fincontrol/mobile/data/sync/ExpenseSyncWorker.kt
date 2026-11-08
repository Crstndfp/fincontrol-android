package com.fincontrol.mobile.data.sync

import android.content.Context
import androidx.room.Room
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fincontrol.mobile.data.local.FinControlDatabase
import com.fincontrol.mobile.data.local.LocalExpenseEntity
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

data class SyncPendingExpense(
    val id: String,
    val merchantName: String,
    val categoryName: String,
    val currency: String,
    val totalAmountOriginal: Double,
    val isSplitExpense: Boolean,
    val splitPercentage: Double,
    val personalAmountGtq: Double,
    val receivableAmountGtq: Double,
    val timestampEpochMillis: Long
)

data class SyncRequest(
    val pendingTransactions: List<SyncPendingExpense>,
    val clientTimestamp: String = java.time.Instant.now().toString()
)

data class SyncResponse(
    val syncSummary: SyncSummary,
    val cachedState: CachedServerState
)

data class SyncSummary(
    val pushedExpensesCount: Int,
    val serverTime: String
)

data class CachedServerState(
    val liquidAssetsGtq: Double,
    val monthlyBudgetCeilingGtq: Double,
    val monthlyPersonalExecutedGtq: Double,
    val semaphoreStatus: String,
    val recentMovements: List<Any>? = null
)

interface FinControlApiService {
    @POST("sync")
    suspend fun executeTwoWaySync(@Body request: SyncRequest): SyncResponse
}

object ApiClientProvider {
    private const val PREFS_NAME = "fincontrol_prefs"
    private const val KEY_SERVER_URL = "server_base_url"
    const val DEFAULT_LOCAL_URL = "http://10.0.2.2:3000/api/v1/"

    fun getServerUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SERVER_URL, DEFAULT_LOCAL_URL) ?: DEFAULT_LOCAL_URL
    }

    fun setServerUrl(context: Context, newUrl: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SERVER_URL, newUrl.trimEnd('/') + "/").apply()
    }

    fun getService(context: Context): FinControlApiService {
        val baseUrl = getServerUrl(context)

        val client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(FinControlApiService::class.java)
    }
}

/**
 * Two-Way Sync Worker:
 * 1. PUSH: Gathers offline records in Room with status PENDING_SYNC and posts them to internal server.
 * 2. PULL: Updates cached liquid assets and budget ceilings from server response.
 * 3. Gracefully retries or catches connection errors without breaking offline experience.
 */
class ExpenseSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val db = Room.databaseBuilder(
            applicationContext,
            FinControlDatabase::class.java,
            "fincontrol_local_db"
        ).build()

        return try {
            val pending = db.expenseDao().getPendingSyncExpenses()
            val payload = pending.map {
                SyncPendingExpense(
                    id = it.id,
                    merchantName = it.merchantName,
                    categoryName = it.categoryName,
                    currency = it.currency,
                    totalAmountOriginal = it.totalAmountOriginal,
                    isSplitExpense = it.isSplitExpense,
                    splitPercentage = it.splitPercentage,
                    personalAmountGtq = it.personalAmountGtq,
                    receivableAmountGtq = it.receivableAmountGtq,
                    timestampEpochMillis = it.timestampEpochMillis
                )
            }

            val api = ApiClientProvider.getService(applicationContext)
            val response = api.executeTwoWaySync(SyncRequest(pendingTransactions = payload))

            // Mark synced records
            for (p in pending) {
                db.expenseDao().updateSyncStatus(p.id, "SYNCED_AWAITING_PDF_RECONCILE")
            }

            // Save last sync timestamp
            val prefs = applicationContext.getSharedPreferences("fincontrol_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("last_sync_time", response.syncSummary.serverTime)
                .putFloat("cached_liquid_gtq", response.cachedState.liquidAssetsGtq.toFloat())
                .putFloat("cached_ceiling_gtq", response.cachedState.monthlyBudgetCeilingGtq.toFloat())
                .putFloat("cached_executed_gtq", response.cachedState.monthlyPersonalExecutedGtq.toFloat())
                .apply()

            Result.success()
        } catch (e: Exception) {
            // Internal server in LAN is unreachable (user is outside the network)
            Result.retry()
        }
    }
}
