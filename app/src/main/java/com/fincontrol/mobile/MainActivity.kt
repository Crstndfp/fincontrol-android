package com.fincontrol.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincontrol.mobile.domain.*
import java.math.BigDecimal

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                FinControlExpressScreen()
            }
        }
    }
}

@Composable
fun FinControlExpressScreen() {
    // Module 3.1: Direct numeric input (< 5 seconds capture)
    var amountInput by remember { mutableStateOf("600") }
    var currency by remember { mutableStateOf(CurrencyCode.GTQ) }
    var merchantInput by remember { mutableStateOf("Restaurante San Martín") }
    var selectedCategory by remember { mutableStateOf("Restaurantes & Cafés") }

    // Module 3.2: Split Expense Toggle & Options (RN-01)
    var isSplitEnabled by remember { mutableStateOf(true) }
    var splitMode by remember { mutableStateOf(SplitMode.PERCENTAGE) }
    var selectedPct by remember { mutableStateOf(BigDecimal("50")) }
    var exactPersonalInput by remember { mutableStateOf("200") }

    // Module 3.3: Mini Dashboard State
    var executedMonthlyPersonalGtq by remember { mutableStateOf(BigDecimal("14250.00")) }
    val monthlyBudgetCeilingGtq = BigDecimal("27200.00")
    val netLiquidityGtq = BigDecimal("70260.00")

    val splitPreview = remember(
        amountInput, currency, isSplitEnabled, splitMode, selectedPct, exactPersonalInput
    ) {
        val parsedAmt = amountInput.toBigDecimalOrNull() ?: BigDecimal.ZERO
        if (parsedAmt <= BigDecimal.ZERO) null
        else {
            runCatching {
                SplitExpenseCalculator.calculate(
                    totalAmountOriginal = parsedAmt,
                    currency = currency,
                    mode = if (!isSplitEnabled) SplitMode.NONE else splitMode,
                    percentageOwn = selectedPct,
                    exactPersonalAmountOriginal = exactPersonalInput.toBigDecimalOrNull()
                )
            }.getOrNull()
        }
    }

    val categories = listOf(
        "Restaurantes & Cafés",
        "Supermercado & Despensa",
        "Transporte & Combustible",
        "Salud & Farmacia",
        "Servicios Básicos & Seguros",
        "Estilo de Vida / General"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Mini Executive Dashboard Header (Module 3.3)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Liquidez Disponible Consolidada", color = Color.LightGray, fontSize = 12.sp)
                            Text(
                                "Q $netLiquidityGtq",
                                color = Color(0xFF34D399),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Corte TC: 10d | Pago: Día 8",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }

                    val semaphore = SplitExpenseCalculator.evaluateSemaphore(
                        executedMonthlyPersonalGtq,
                        monthlyBudgetCeilingGtq
                    )
                    val semColor = when (semaphore) {
                        SemaphoreColor.GREEN -> Color(0xFF34D399)
                        SemaphoreColor.YELLOW -> Color(0xFFFBBF24)
                        SemaphoreColor.RED -> Color(0xFFF87171)
                    }

                    Text(
                        "Semáforo Mensual 50/30/20: Q $executedMonthlyPersonalGtq / Q $monthlyBudgetCeilingGtq",
                        color = semColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Express Capture Form (< 5 seconds - Module 3.1)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Registro Express (<5s • RN-03 Sin NIT)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CurrencyCode.values().forEach { curr ->
                                FilterChip(
                                    selected = currency == curr,
                                    onClick = { currency = curr },
                                    label = { Text(curr.name) }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("1. Monto Pagado Total (${currency.name})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = merchantInput,
                        onValueChange = {
                            merchantInput = it
                            selectedCategory = SplitExpenseCalculator.predictCategory(it)
                        },
                        label = { Text("2. Comercio (Autocompletado Predictivo)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("3. Categoría Predictiva:", color = Color.LightGray, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.take(3).forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }

                    // Module 3.2: Split Expenses Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("¿Es un gasto compartido? (RN-01)", color = Color.White, fontSize = 14.sp)
                        Switch(checked = isSplitEnabled, onCheckedChange = { isSplitEnabled = it })
                    }

                    if (isSplitEnabled) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("50", "33", "25").forEach { pctStr ->
                                val bd = BigDecimal(pctStr)
                                FilterChip(
                                    selected = splitMode == SplitMode.PERCENTAGE && selectedPct == bd,
                                    onClick = {
                                        splitMode = SplitMode.PERCENTAGE
                                        selectedPct = bd
                                    },
                                    label = { Text("$pctStr%") }
                                )
                            }
                            FilterChip(
                                selected = splitMode == SplitMode.EXACT_PERSONAL_AMOUNT,
                                onClick = { splitMode = SplitMode.EXACT_PERSONAL_AMOUNT },
                                label = { Text("Monto Propio") }
                            )
                        }

                        if (splitMode == SplitMode.EXACT_PERSONAL_AMOUNT) {
                            OutlinedTextField(
                                value = exactPersonalInput,
                                onValueChange = { exactPersonalInput = it },
                                label = { Text("Mi parte asumida propia (${currency.name})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        splitPreview?.let { calc ->
                            Surface(
                                color = Color(0xFF1E1B4B),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "Gasto Personal Real (Presupuesto): Q ${calc.personalAmountGtq}",
                                        color = Color(0xFF34D399),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        "Cuenta por Cobrar a Terceros (Activo): Q ${calc.receivableAmountGtq}",
                                        color = Color(0xFFFBBF24),
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            splitPreview?.let {
                                executedMonthlyPersonalGtq = executedMonthlyPersonalGtq.add(it.personalAmountGtq)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Guardar Gasto Express y Sincronizar")
                    }
                }
            }
        }
    }
}
