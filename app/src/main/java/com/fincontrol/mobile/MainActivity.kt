package com.fincontrol.mobile

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fincontrol.mobile.domain.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

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

data class RecentMovement(
    val id: String = UUID.randomUUID().toString(),
    val merchantName: String,
    val categoryName: String,
    val totalAmount: BigDecimal,
    val personalAmount: BigDecimal,
    val receivableAmount: BigDecimal,
    val currency: CurrencyCode,
    val dateFormatted: String,
    val hasAttachment: Boolean = false,
    val isSplit: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinControlExpressScreen() {
    val context = LocalContext.current

    // Module 3.1: Direct numeric input (< 5 seconds capture)
    var amountInput by remember { mutableStateOf("600") }
    var currency by remember { mutableStateOf(CurrencyCode.GTQ) }
    var merchantInput by remember { mutableStateOf("Restaurante San Martín") }
    var selectedCategory by remember { mutableStateOf("Restaurantes & Cafés") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var attachedPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Module 3.2: Split Expense Toggle & Options (RN-01)
    var isSplitEnabled by remember { mutableStateOf(true) }
    var splitMode by remember { mutableStateOf(SplitMode.PERCENTAGE) }
    var selectedPct by remember { mutableStateOf(BigDecimal("50")) }
    var exactPersonalInput by remember { mutableStateOf("200") }

    // Module 3.3: Mini Dashboard State
    var executedMonthlyPersonalGtq by remember { mutableStateOf(BigDecimal("14250.00")) }
    val monthlyBudgetCeilingGtq = BigDecimal("27200.00")
    val netLiquidityGtq = BigDecimal("70260.00")

    // Last 5 movements with Swipe-to-Delete / Edit
    val recentMovements = remember {
        mutableStateListOf(
            RecentMovement(
                merchantName = "Restaurante San Martín",
                categoryName = "Restaurantes & Cafés",
                totalAmount = BigDecimal("600.00"),
                personalAmount = BigDecimal("300.00"),
                receivableAmount = BigDecimal("300.00"),
                currency = CurrencyCode.GTQ,
                dateFormatted = "Hoy 13:40",
                isSplit = true
            ),
            RecentMovement(
                merchantName = "Supermercados La Torre",
                categoryName = "Supermercado & Despensa",
                totalAmount = BigDecimal("500.00"),
                personalAmount = BigDecimal("200.00"),
                receivableAmount = BigDecimal("300.00"),
                currency = CurrencyCode.GTQ,
                dateFormatted = "Ayer 19:15",
                isSplit = true
            ),
            RecentMovement(
                merchantName = "Gasolinera Texaco",
                categoryName = "Transporte & Combustible",
                totalAmount = BigDecimal("420.00"),
                personalAmount = BigDecimal("420.00"),
                receivableAmount = BigDecimal("0.00"),
                currency = CurrencyCode.GTQ,
                dateFormatted = "06 Nov",
                isSplit = false
            ),
            RecentMovement(
                merchantName = "Farmacia Galeno",
                categoryName = "Salud & Farmacia",
                totalAmount = BigDecimal("150.00"),
                personalAmount = BigDecimal("150.00"),
                receivableAmount = BigDecimal("0.00"),
                currency = CurrencyCode.GTQ,
                dateFormatted = "04 Nov",
                hasAttachment = true,
                isSplit = false
            ),
            RecentMovement(
                merchantName = "Amazon AWS Services",
                categoryName = "Entretenimiento & Suscripciones",
                totalAmount = BigDecimal("45.00"),
                personalAmount = BigDecimal("351.00"),
                receivableAmount = BigDecimal("0.00"),
                currency = CurrencyCode.USD,
                dateFormatted = "02 Nov",
                isSplit = false
            )
        )
    }

    // Photo picker launcher (RN-03: Attachment optional)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        attachedPhotoUri = uri
        if (uri != null) {
            Toast.makeText(context, "Foto de factura adjuntada", Toast.LENGTH_SHORT).show()
        }
    }

    // Date picker dialog
    val datePickerDialog = remember {
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        )
    }

    val totaleroStatus = remember {
        SplitExpenseCalculator.calculateTotaleroCountdown(cutoffDay = 18, paymentDueDay = 8)
    }

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
                                text = "Corte: ${totaleroStatus.daysUntilCutoff}d | Pago: ${totaleroStatus.daysUntilPaymentDue}d (0%)",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
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

                    // 1. Amount
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it },
                        label = { Text("1. Monto Pagado Total (${currency.name})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 2. Merchant with Autocomplete
                    OutlinedTextField(
                        value = merchantInput,
                        onValueChange = {
                            merchantInput = it
                            selectedCategory = SplitExpenseCalculator.predictCategory(it)
                        },
                        label = { Text("2. Comercio (Autocompletado Predictivo)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 3. Category Chips
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

                    // 4. Date & Attachment Bar (Module 3.1 - Fields 4 and 5)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { datePickerDialog.show() },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF818CF8))
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Fecha", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Fecha: $selectedDate", fontSize = 12.sp)
                        }

                        IconButton(
                            onClick = { photoPickerLauncher.launch("image/*") }
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Adjuntar Foto Factura",
                                tint = if (attachedPhotoUri != null) Color(0xFF34D399) else Color.LightGray
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
                                recentMovements.add(
                                    0,
                                    RecentMovement(
                                        merchantName = merchantInput,
                                        categoryName = selectedCategory,
                                        totalAmount = it.totalAmountOriginal,
                                        personalAmount = it.personalAmountGtq,
                                        receivableAmount = it.receivableAmountGtq,
                                        currency = currency,
                                        dateFormatted = "Justo ahora",
                                        hasAttachment = attachedPhotoUri != null,
                                        isSplit = isSplitEnabled
                                    )
                                )
                                if (recentMovements.size > 5) {
                                    recentMovements.removeLast()
                                }
                                Toast.makeText(context, "Gasto guardado (< 5s)", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Guardar Gasto Express y Sincronizar")
                    }
                }
            }
        }

        // Module 3.3: Últimos 5 Movimientos con opción de deslizar / eliminar
        item {
            Text(
                "Últimos 5 Movimientos (Desliza para eliminar / auditar)",
                color = Color.LightGray,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        items(recentMovements, key = { it.id }) { item ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = { dismissValue ->
                    if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                        recentMovements.remove(item)
                        Toast.makeText(context, "Movimiento eliminado", Toast.LENGTH_SHORT).show()
                        true
                    } else false
                }
            )

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    val color by animateColorAsState(
                        if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) Color(0xFFEF4444)
                        else Color.Transparent,
                        label = "dismissColor"
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color, RoundedCornerShape(16.dp))
                            .padding(horizontal = 20.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            tint = Color.White
                        )
                    }
                }
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.merchantName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                "${item.categoryName} • ${item.dateFormatted}",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                            if (item.isSplit) {
                                Text(
                                    "Por cobrar: Q ${item.receivableAmount}",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "Q ${item.personalAmount}",
                                color = Color(0xFF34D399),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            if (item.currency == CurrencyCode.USD) {
                                Text(
                                    "$ ${item.totalAmount} USD",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 11.sp
                                )
                            }
                            if (item.hasAttachment) {
                                Icon(
                                    Icons.Default.Receipt,
                                    contentDescription = "Factura adjunta",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
