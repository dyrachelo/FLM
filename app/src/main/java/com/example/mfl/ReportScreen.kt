package com.example.mfl

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
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
import java.util.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions

@SuppressLint("SimpleDateFormat")
@Composable
fun ReportScreen(viewModel: ReportViewModel) {
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var monthlyLimitInput by remember { mutableStateOf("") }
    var showPieChart by remember { mutableStateOf(true) }
    var showPeriodSelector by remember { mutableStateOf(false) }

    // Загружаем данные при первом отображении
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
        // Блок с бюджетом и лимитом
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Общие расходы
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Общие расходы:", style = MaterialTheme.typography.subtitle1)
                    Text(
                        "${"%.2f".format(viewModel.totalExpenses)} Br",
                        style = MaterialTheme.typography.subtitle1
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Блок месячного лимита
                if (viewModel.monthlyGoal > 0) {
                    val isExceeded = viewModel.totalExpenses > viewModel.monthlyGoal
                    val progress = (viewModel.totalExpenses / viewModel.monthlyGoal).toFloat()

                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Месячный лимит:", style = MaterialTheme.typography.body1)

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${"%.2f".format(viewModel.monthlyGoal)} Br",
                                    color = if (isExceeded) Color.Red else MaterialTheme.colors.onSurface,
                                    style = MaterialTheme.typography.body1.copy(
                                        fontWeight = if (isExceeded) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                                IconButton(
                                    onClick = {
                                        monthlyLimitInput = viewModel.monthlyGoal.toString()
                                        showAddGoalDialog = true
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Изменить лимит")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = progress.coerceAtMost(1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp),
                            color = if (isExceeded) Color.Red else MaterialTheme.colors.primary,
                            backgroundColor = MaterialTheme.colors.onSurface.copy(alpha = 0.1f)
                        )

                        if (isExceeded) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "ПРЕВЫШЕНО НА ${"%.2f".format(viewModel.totalExpenses - viewModel.monthlyGoal)} Br",
                                color = Color.Red,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                } else {
                    Button(
                        onClick = { showAddGoalDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Установить месячный лимит")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка выбора периода
        OutlinedButton(
            onClick = { showPeriodSelector = !showPeriodSelector },
            modifier = Modifier.fillMaxWidth()
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

        // Графики расходов
        if (viewModel.expenses.isNotEmpty()) {
            // Кнопка переключения графиков
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { showPieChart = !showPieChart },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (showPieChart) Color.Gray else MaterialTheme.colors.primary
                    ),
                    modifier = Modifier.padding(8.dp)
                ) {
                    Text(if (showPieChart) "Линейный график" else "Круговая диаграмма")
                }
            }

            // Отображение выбранного графика
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = 4.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        if (showPieChart) "Расходы по категориям" else "Динамика расходов",
                        style = MaterialTheme.typography.h6
                    )

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
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    // Диалог установки/изменения лимита
    if (showAddGoalDialog) {
        AlertDialog(
            onDismissRequest = { showAddGoalDialog = false },
            title = { Text(if (viewModel.monthlyGoal > 0) "Изменить лимит" else "Установить лимит") },
            text = {
                Column {
                    OutlinedTextField(
                        value = monthlyLimitInput,
                        onValueChange = { monthlyLimitInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Лимит расходов (Br)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "Текущие расходы: ${"%.2f".format(viewModel.totalExpenses)} Br",
                        modifier = Modifier.padding(top = 8.dp)
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
                    enabled = monthlyLimitInput.isNotBlank()
                ) {
                    Text(if (viewModel.monthlyGoal > 0) "Обновить" else "Сохранить")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddGoalDialog = false }
                ) {
                    Text("Отмена")
                }
            }
        )
    }
}





@Composable
fun ExpensePieChart(data: Map<String, Double>) {
    AndroidView(
        factory = { context ->
            PieChart(context).apply {
                val entries = data.map { PieEntry(it.value.toFloat(), it.key) }
                val set = PieDataSet(entries, "Категории").apply {
                    colors = ColorTemplate.MATERIAL_COLORS.toList()
                    valueTextSize = 12f
                }
                this.data = PieData(set)
                description.isEnabled = false
                legend.isEnabled = true
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
    // Группируем расходы по дням и сортируем по дате
    val dailyExpenses = transactions
        .filter { it["type"] == "expense" }
        .groupBy { it["date"].toString().substring(0, 10) } // Группируем по дате (YYYY-MM-DD)
        .mapValues { (_, expenses) -> expenses.sumOf { it["amount"] as Double } }
        .toSortedMap() // Сортируем по дате

    // Создаем список меток для оси X (дни)
    val dates = dailyExpenses.keys.toList()

    AndroidView(
        factory = { context ->
            LineChart(context).apply {
                // Создаем точки данных
                val entries = dailyExpenses.values.mapIndexed { index, amount ->
                    Entry(index.toFloat(), amount.toFloat())
                }

                val dataSet = LineDataSet(entries, "Ежедневные расходы").apply {
                    color = android.graphics.Color.BLUE
                    lineWidth = 2f
                    valueTextSize = 10f
                    setDrawValues(true)
                    mode = LineDataSet.Mode.CUBIC_BEZIER // Плавные линии
                    setDrawFilled(true) // Заливка под графиком
                    fillColor = android.graphics.Color.BLUE
                    fillAlpha = 50
                }

                this.data = LineData(dataSet)

                // Настраиваем ось X
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                        override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                            val index = value.toInt()
                            return if (index in dates.indices) {
                                dates[index].substring(8) // Показываем только день (DD)
                            } else {
                                ""
                            }
                        }
                    }
                    granularity = 1f
                    setDrawGridLines(false)
                }

                // Настраиваем ось Y
                axisLeft.apply {
                    setDrawGridLines(true)
                    axisMinimum = 0f // Начинаем с 0
                }

                axisRight.isEnabled = false
                description.isEnabled = false
                legend.isEnabled = true

                // Анимация
                animateX(1000)
                animateY(1000)
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    )
}

@Composable
fun PeriodSelector(selected: String, onSelect: (String) -> Unit) {
    val periods = listOf(
        "Week" to "week",
        "Month" to "month",
        "Quarter" to "quarter",
        "Half year" to "halfyear",
        "Year" to "year",
        //"All period" to "all"
    )

    Column(modifier = Modifier.padding(16.dp)) {
        periods.forEach { (displayName, periodKey) ->
            Text(
                text = displayName,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(periodKey) }
                    .padding(vertical = 8.dp),
                color = if (selected == periodKey) MaterialTheme.colors.primary else MaterialTheme.colors.onSurface
            )
            if (periodKey != "all") Divider()
        }
    }
}

private fun getCurrentMonth(): String {
    val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    return dateFormat.format(Date())
}