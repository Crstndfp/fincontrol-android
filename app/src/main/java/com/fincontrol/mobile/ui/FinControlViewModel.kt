package com.fincontrol.mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fincontrol.mobile.data.local.LocalExpenseEntity
import com.fincontrol.mobile.data.repository.ExpenseRepository
import com.fincontrol.mobile.domain.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate

data class FinControlUiState(
    val liquidAssetsGtq: BigDecimal = BigDecimal("70260.00"),
    val monthlyBudgetCeilingGtq: BigDecimal = BigDecimal("27200.00"),
    val monthlyPersonalExecutedGtq: BigDecimal = BigDecimal("14250.00"),
    val totaleroStatus: TotaleroStatus = SplitExpenseCalculator.calculateTotaleroCountdown(),
    val semaphoreColor: SemaphoreColor = SemaphoreColor.GREEN,
    val recentExpenses: List<LocalExpenseEntity> = emptyList(),
    val isSyncing: Boolean = false
)

class FinControlViewModel(
    private val repository: ExpenseRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinControlUiState())
    val uiState: StateFlow<FinControlUiState> = _uiState.asStateFlow()

    init {
        updateSemaphore()
        observeLocalExpenses()
    }

    private fun observeLocalExpenses() {
        viewModelScope.launch {
            repository?.observeRecentExpenses()?.collect { expenses ->
                _uiState.update { it.copy(recentExpenses = expenses) }
            }
        }
    }

    private fun updateSemaphore() {
        val sem = SplitExpenseCalculator.evaluateSemaphore(
            _uiState.value.monthlyPersonalExecutedGtq,
            _uiState.value.monthlyBudgetCeilingGtq
        )
        _uiState.update { it.copy(semaphoreColor = sem) }
    }

    fun recordExpressExpense(
        merchantName: String,
        categoryName: String,
        currency: CurrencyCode,
        splitResult: SplitCalculationResult,
        attachmentUri: String? = null
    ) {
        viewModelScope.launch {
            val entity = LocalExpenseEntity(
                merchantName = merchantName,
                categoryName = categoryName,
                currency = currency.name,
                totalAmountOriginal = splitResult.totalAmountOriginal.toDouble(),
                totalAmountGtq = splitResult.totalAmountGtq.toDouble(),
                isSplitExpense = splitResult.isSplitExpense,
                splitPercentage = splitResult.appliedPercentage.toDouble(),
                personalAmountGtq = splitResult.personalAmountGtq.toDouble(),
                receivableAmountGtq = splitResult.receivableAmountGtq.toDouble(),
                attachmentUri = attachmentUri,
                syncStatus = "PENDING_SYNC"
            )

            repository?.saveExpenseLocally(entity)

            // Increment local monthly execution immediately for real-time visual feedback
            _uiState.update { current ->
                val newExecuted = current.monthlyPersonalExecutedGtq.add(splitResult.personalAmountGtq)
                current.copy(
                    monthlyPersonalExecutedGtq = newExecuted,
                    semaphoreColor = SplitExpenseCalculator.evaluateSemaphore(newExecuted, current.monthlyBudgetCeilingGtq)
                )
            }
        }
    }

    fun removeExpense(id: String, personalAmountGtq: Double) {
        viewModelScope.launch {
            repository?.deleteExpense(id)
            _uiState.update { current ->
                val newExecuted = current.monthlyPersonalExecutedGtq.subtract(BigDecimal.valueOf(personalAmountGtq)).coerceAtLeast(BigDecimal.ZERO)
                current.copy(
                    monthlyPersonalExecutedGtq = newExecuted,
                    semaphoreColor = SplitExpenseCalculator.evaluateSemaphore(newExecuted, current.monthlyBudgetCeilingGtq)
                )
            }
        }
    }
}
