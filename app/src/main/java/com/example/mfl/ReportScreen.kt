package com.example.mfl

import android.annotation.SuppressLint
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import java.text.SimpleDateFormat
import androidx.compose.ui.graphics.toArgb // Import this
import java.util.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SimpleDateFormat")
@Composable
fun ReportScreen(viewModel: ReportViewModel) {
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var monthlyLimitInput by remember { mutableStateOf("") }
    var showPieChart by remember { mutableStateOf(true) }
    var showPeriodSelector by remember { mutableStateOf(false) }

    // Custom colors
    val buttonColor = Color.Black
    val buttonTextColor = Color.White
    val cardBackground = Color.White
    val dividerColor = Color.LightGray
    val textColor = Color.Black
    val exceededColor = Color.Red

    LaunchedEffect(Unit) {
        viewModel.loadMonthlyGoal()
        viewModel.loadExpensesForPeriod(viewModel.selectedPeriod)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // Budget and limit block
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = cardBackground),
            shape = MaterialTheme.shapes.extraSmall,
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Общие расходы:",
                        style = MaterialTheme.typography.titleSmall,
                        color = textColor)
                    Text(
                        "${"%.2f".format(viewModel.totalExpenses)} Br",
                        style = MaterialTheme.typography.titleSmall,
                        color = textColor
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (viewModel.monthlyGoal > 0) {
                    val isExceeded = viewModel.totalExpenses > viewModel.monthlyGoal
                    val progress = (viewModel.totalExpenses / viewModel.monthlyGoal).toFloat()

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Месячный лимит:",
                                style = MaterialTheme.typography.bodyLarge,
                                color = textColor)

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${"%.2f".format(viewModel.monthlyGoal)} Br",
                                    color = if (isExceeded) exceededColor else textColor,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                IconButton(
                                    onClick = {
                                        monthlyLimitInput = viewModel.monthlyGoal.toString()
                                        showAddGoalDialog = true
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Edit,
                                        contentDescription = "Изменить лимит",
                                        tint = textColor)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { progress.coerceAtMost(1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp),
                            color = if (isExceeded) exceededColor else buttonColor,
                            trackColor = dividerColor
                        )

                        if (isExceeded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "ПРЕВЫШЕНО НА ${"%.2f".format(viewModel.totalExpenses - viewModel.monthlyGoal)} Br",
                                color = exceededColor,
                                modifier = Modifier.align(Alignment.End),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { showAddGoalDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonColor,
                            contentColor = buttonTextColor
                        ),
                        shape = MaterialTheme.shapes.extraSmall
                    ) {
                        Text("Установить месячный лимит")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = { showPeriodSelector = !showPeriodSelector },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = buttonTextColor,
                containerColor = buttonColor
            ),
            shape = MaterialTheme.shapes.extraSmall,
            border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp)
        ) {
            Text(viewModel.getFormattedPeriod(viewModel.selectedPeriod))
        }

        if (showPeriodSelector) {
            PeriodSelector(
                selected = viewModel.selectedPeriod,
                onSelect = { period ->
                    viewModel.loadExpensesForPeriod(period)
                    showPeriodSelector = false
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (viewModel.expenses.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { showPieChart = !showPieChart },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = buttonTextColor
                    ),
                    modifier = Modifier.padding(8.dp),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(if (showPieChart) "Круговая диаграмма" else "Линейный график")
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                shape = MaterialTheme.shapes.extraSmall,
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        if (showPieChart) "Расходы по категориям" else "Динамика расходов",
                        style = MaterialTheme.typography.titleSmall,
                        color = textColor
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (showPieChart) {
                        ExpensePieChart(viewModel.expenses)
                    } else {
                        ExpenseGrowthChart(viewModel.expensesRaw)
                    }
                }
            }
        } else {
            Text(
                "Нет данных за выбранный период",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = textColor
            )
        }
    }

    if (showAddGoalDialog) {
        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            title = {
                Text(if (viewModel.monthlyGoal > 0) "Изменить лимит" else "Установить лимит",
                    color = textColor)
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = monthlyLimitInput,
                        onValueChange = { monthlyLimitInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Лимит расходов (Br)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            unfocusedBorderColor = dividerColor,
                            focusedBorderColor = buttonColor
                        ),
                        textStyle = LocalTextStyle.current.copy(color = textColor)
                    )
                    Text(
                        "Текущие расходы: ${"%.2f".format(viewModel.totalExpenses)} Br",
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        monthlyLimitInput.toDoubleOrNull()?.let { limit ->
                            viewModel.setMonthlyGoal(limit)
                            showAddGoalDialog = false
                            monthlyLimitInput = ""
                        }
                    },
                    enabled = monthlyLimitInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonColor,
                        contentColor = buttonTextColor
                    ),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(if (viewModel.monthlyGoal > 0) "Обновить" else "Сохранить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddGoalDialog = false },
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text("Отмена", color = textColor)
                }
            },
            containerColor = cardBackground
        )
    }
}

@Composable
fun PeriodSelector(selected: String, onSelect: (String) -> Unit) {
    val periods = listOf(
        "Week" to "week",
        "Month" to "month",
        "Quarter" to "quarter",
        "Half year" to "halfyear",
        "Year" to "year",
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = MaterialTheme.shapes.extraSmall,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            periods.forEach { (displayName, periodKey) ->
                Text(
                    text = displayName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(periodKey) }
                        .padding(vertical = 8.dp),
                    color = if (selected == periodKey) Color.Black else Color.DarkGray
                )
                if (periodKey != "year") Divider(color = Color.LightGray, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun ExpensePieChart(data: Map<String, Double>) {
    AndroidView(
        factory = { context ->
            PieChart(context).apply {
                val entries = data.map { PieEntry(it.value.toFloat(), it.key) }
                val set = PieDataSet(entries, "Категории").apply {
                    colors = listOf(
                        Color(0xFF1E88E5).toArgb(), // Синий
                        Color(0xFF43A047).toArgb(), // Зеленый
                        Color(0xFFFDD835).toArgb(), // Желтый
                        Color(0xFFE53935).toArgb(), // Красный
                        Color(0xFF8E24AA).toArgb(), // Фиолетовый
                        Color(0xFFFB8C00).toArgb(), // Оранжевый
                        Color(0xFF26C6DA).toArgb(), // Бирюзовый
                        Color(0xFFAB47BC).toArgb()
                    )
                    valueTextSize = 12f
                    valueTextColor = android.graphics.Color.BLACK
                }
                this.data = PieData(set)
                description.isEnabled = false
                legend.textColor = android.graphics.Color.BLACK
                setDrawEntryLabels(false)
                animateY(1000)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    )
}

@Composable
fun ExpenseGrowthChart(transactions: List<Map<String, Any>>) {
    val dailyExpenses = transactions
        .filter { it["type"] == "expense" }
        .groupBy { it["date"].toString().substring(0, 10) }
        .mapValues { (_, expenses) -> expenses.sumOf { it["amount"] as Double } }
        .toSortedMap()

    val dates = dailyExpenses.keys.toList()

    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                val entries = dailyExpenses.values.mapIndexed { index, amount ->
                    Entry(index.toFloat(), amount.toFloat())
                }

                val dataSet = LineDataSet(entries, "Ежедневные расходы").apply {
                    color = android.graphics.Color.BLACK
                    lineWidth = 2f
                    valueTextColor = android.graphics.Color.BLACK
                    setDrawValues(true)
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                    setDrawFilled(true)
                    fillColor = android.graphics.Color.BLACK
                    fillAlpha = 30
                }

                this.data = LineData(dataSet)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                            val index = value.toInt()
                            return if (index in dates.indices) {
                                dates[index].substring(8)
                            } else {
                                ""
                            }
                        }
                    }
                    textColor = android.graphics.Color.BLACK
                    granularity = 1f
                    setDrawGridLines(false)
                }

                axisLeft.apply {
                    setDrawGridLines(true)
                    axisMinimum = 0f
                    textColor = android.graphics.Color.BLACK
                    gridColor = android.graphics.Color.LTGRAY
                }

                axisRight.isEnabled = false
                description.isEnabled = false
                legend.textColor = android.graphics.Color.BLACK

                animateX(1000)
                animateY(1000)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    )
}