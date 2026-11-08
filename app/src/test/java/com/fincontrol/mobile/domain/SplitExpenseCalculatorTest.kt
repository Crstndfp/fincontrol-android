package com.fincontrol.mobile.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class SplitExpenseCalculatorTest {

    @Test
    fun `Scenario 1 - 50 percent shared expense of Q600 yields Q300 personal and Q300 receivable`() {
        val result = SplitExpenseCalculator.calculate(
            totalAmountOriginal = BigDecimal("600.00"),
            currency = CurrencyCode.GTQ,
            mode = SplitMode.PERCENTAGE,
            percentageOwn = BigDecimal("50")
        )

        assertEquals(BigDecimal("600.00"), result.totalAmountGtq)
        assertEquals(BigDecimal("300.00"), result.personalAmountGtq)
        assertEquals(BigDecimal("300.00"), result.receivableAmountGtq)
        assertTrue(result.isSplitExpense)
    }

    @Test
    fun `Module 3_2 - Exact assumed personal amount of Q200 from Q500 yields Q300 receivable`() {
        val result = SplitExpenseCalculator.calculate(
            totalAmountOriginal = BigDecimal("500.00"),
            currency = CurrencyCode.GTQ,
            mode = SplitMode.EXACT_PERSONAL_AMOUNT,
            exactPersonalAmountOriginal = BigDecimal("200.00")
        )

        assertEquals(BigDecimal("500.00"), result.totalAmountGtq)
        assertEquals(BigDecimal("200.00"), result.personalAmountGtq)
        assertEquals(BigDecimal("300.00"), result.receivableAmountGtq)
        assertEquals(BigDecimal("40.00"), result.appliedPercentage)
    }

    @Test
    fun `Module 3_1 - Predictive merchant categorization selects expected category`() {
        assertEquals("Supermercado & Despensa", SplitExpenseCalculator.predictCategory("La Torre Zona 15"))
        assertEquals("Transporte & Combustible", SplitExpenseCalculator.predictCategory("Gasolinera Texaco"))
        assertEquals("Restaurantes & Cafés", SplitExpenseCalculator.predictCategory("Panadería San Martín"))
    }

    @Test
    fun `Module 3_3 - 50-30-20 Semaphore transitions between GREEN, YELLOW and RED`() {
        val ceiling = BigDecimal("1000.00")
        assertEquals(SemaphoreColor.GREEN, SplitExpenseCalculator.evaluateSemaphore(BigDecimal("799.99"), ceiling))
        assertEquals(SemaphoreColor.YELLOW, SplitExpenseCalculator.evaluateSemaphore(BigDecimal("850.00"), ceiling))
        assertEquals(SemaphoreColor.RED, SplitExpenseCalculator.evaluateSemaphore(BigDecimal("1050.00"), ceiling))
    }
}
