package com.fincontrol.mobile.domain

import java.math.BigDecimal
import java.math.RoundingMode

enum class CurrencyCode {
    GTQ, USD
}

enum class SplitMode {
    NONE,
    PERCENTAGE,
    EXACT_PERSONAL_AMOUNT
}

enum class SemaphoreColor {
    GREEN, YELLOW, RED
}

data class SplitCalculationResult(
    val totalAmountOriginal: BigDecimal,
    val totalAmountGtq: BigDecimal,
    val personalAmountGtq: BigDecimal,
    val receivableAmountGtq: BigDecimal,
    val appliedPercentage: BigDecimal,
    val isSplitExpense: Boolean
)

/**
 * Domain Engine for Mobile Express Capture & Split Expenses (Modules 3.1, 3.2 & Scenario 1).
 * Uses BigDecimal with HALF_UP rounding to 2 decimal places (RN-01, RN-03, RN-05).
 */
object SplitExpenseCalculator {

    private val DEFAULT_USD_RATE = BigDecimal("7.80")
    private val ONE_HUNDRED = BigDecimal("100.00")

    fun calculate(
        totalAmountOriginal: BigDecimal,
        currency: CurrencyCode = CurrencyCode.GTQ,
        exchangeRateToGtq: BigDecimal = DEFAULT_USD_RATE,
        mode: SplitMode = SplitMode.NONE,
        percentageOwn: BigDecimal? = null,
        exactPersonalAmountOriginal: BigDecimal? = null
    ): SplitCalculationResult {
        require(totalAmountOriginal > BigDecimal.ZERO) { "Total amount must be greater than zero." }

        val rate = if (currency == CurrencyCode.USD) exchangeRateToGtq else BigDecimal.ONE
        val totalGtq = totalAmountOriginal.multiply(rate).setScale(2, RoundingMode.HALF_UP)

        return when (mode) {
            SplitMode.NONE -> SplitCalculationResult(
                totalAmountOriginal = totalAmountOriginal.setScale(2, RoundingMode.HALF_UP),
                totalAmountGtq = totalGtq,
                personalAmountGtq = totalGtq,
                receivableAmountGtq = BigDecimal.ZERO.setScale(2),
                appliedPercentage = ONE_HUNDRED,
                isSplitExpense = false
            )

            SplitMode.PERCENTAGE -> {
                val pct = percentageOwn ?: ONE_HUNDRED
                require(pct > BigDecimal.ZERO && pct <= ONE_HUNDRED) {
                    "Split percentage must be between 0.01 and 100."
                }
                val personalGtq = totalGtq.multiply(pct)
                    .divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP)
                val receivableGtq = totalGtq.subtract(personalGtq).setScale(2, RoundingMode.HALF_UP)

                SplitCalculationResult(
                    totalAmountOriginal = totalAmountOriginal.setScale(2, RoundingMode.HALF_UP),
                    totalAmountGtq = totalGtq,
                    personalAmountGtq = personalGtq,
                    receivableAmountGtq = receivableGtq,
                    appliedPercentage = pct.setScale(2, RoundingMode.HALF_UP),
                    isSplitExpense = pct < ONE_HUNDRED
                )
            }

            SplitMode.EXACT_PERSONAL_AMOUNT -> {
                val exactOrig = exactPersonalAmountOriginal ?: totalAmountOriginal
                require(exactOrig >= BigDecimal.ZERO && exactOrig <= totalAmountOriginal) {
                    "Personal assumed amount cannot exceed total paid amount."
                }
                val personalGtq = exactOrig.multiply(rate).setScale(2, RoundingMode.HALF_UP)
                val receivableGtq = totalGtq.subtract(personalGtq).setScale(2, RoundingMode.HALF_UP)
                val derivedPct = personalGtq.multiply(ONE_HUNDRED)
                    .divide(totalGtq, 2, RoundingMode.HALF_UP)

                SplitCalculationResult(
                    totalAmountOriginal = totalAmountOriginal.setScale(2, RoundingMode.HALF_UP),
                    totalAmountGtq = totalGtq,
                    personalAmountGtq = personalGtq,
                    receivableAmountGtq = receivableGtq,
                    appliedPercentage = derivedPct,
                    isSplitExpense = receivableGtq > BigDecimal.ZERO
                )
            }
        }
    }

    /**
     * Predictive Merchant Category Matcher (Module 3.1)
     */
    private val defaultKeywordRules = mapOf(
        listOf("LA TORRE", "WALMART", "PAIZ", "PRICESMART") to "Supermercado & Despensa",
        listOf("TEXACO", "SHELL", "PUMA", "UNO") to "Transporte & Combustible",
        listOf("SAN MARTIN", "TRE FRATELLI", "STARBUCKS", "KFC") to "Restaurantes & Cafés",
        listOf("FARMACIA GALENO", "CRUZ VERDE", "MEYKOS") to "Salud & Farmacia",
        listOf("EEGSA", "TIGO", "CLARO") to "Servicios Básicos & Seguros"
    )

    fun predictCategory(merchantInput: String): String {
        val normalized = merchantInput.uppercase()
        for ((keywords, category) in defaultKeywordRules) {
            if (keywords.any { normalized.contains(it) }) {
                return category
            }
        }
        return "Estilo de Vida / General"
    }

    /**
     * 50/30/20 Semaphore Evaluator (Module 3.3)
     */
    fun evaluateSemaphore(executedPersonalGtq: BigDecimal, monthlyCeilingGtq: BigDecimal): SemaphoreColor {
        if (monthlyCeilingGtq <= BigDecimal.ZERO) return SemaphoreColor.GREEN
        val ratio = executedPersonalGtq.divide(monthlyCeilingGtq, 4, RoundingMode.HALF_UP)
        return when {
            ratio > BigDecimal.ONE -> SemaphoreColor.RED
            ratio >= BigDecimal("0.80") -> SemaphoreColor.YELLOW
            else -> SemaphoreColor.GREEN
        }
    }
}
