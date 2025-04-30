package com.example.mfl

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import java.util.*
import java.text.SimpleDateFormat
import android.util.Log


class ReportViewModel(private val dbHelper: DbHelper, private val email: String) : ViewModel() {
    private var _monthlyGoal by mutableStateOf(0.0)
    val monthlyGoal: Double get() = _monthlyGoal

    private var _totalExpenses by mutableStateOf(0.0)
    val totalExpenses: Double get() = _totalExpenses



    private var _selectedPeriod by mutableStateOf("month")
    val selectedPeriod: String get() = _selectedPeriod

    var expenses by mutableStateOf<Map<String, Double>>(emptyMap())
        private set

    var expensesRaw by mutableStateOf<List<Map<String, Any>>>(emptyList())
        private set


    fun loadMonthlyGoal() {
        // Загрузка цели из БД
        _monthlyGoal = dbHelper.getMonthlyGoal(email)
        // Расчет общих расходов за текущий месяц
        calculateTotalExpenses()
    }

    fun setMonthlyGoal(limit: Double) {
        dbHelper.setMonthlyGoal(email, limit)
        _monthlyGoal = limit
    }

    private fun calculateTotalExpenses() {
        // Расчет суммы всех расходов за текущий месяц
        _totalExpenses = expenses.values.sum()
    }
    val goals = mutableStateListOf<Goal>()

    fun loadGoals() {
        val currentEmail = email?.takeIf { it.isNotBlank() }
        if (currentEmail == null) {
            Log.e("loadGoals", "Email is null or blank. Cannot load goals.")
            return
        }

        val fetchedGoals = dbHelper?.getGoals(currentEmail)
        if (fetchedGoals != null) {
            goals.clear()
            goals.addAll(fetchedGoals)
        } else {
            Log.w("loadGoals", "No goals found for user: $currentEmail")
        }
    }



    fun addGoal(category: String, limit: Double, period: String) {
        dbHelper.setGoal(email, category, limit, period)
        loadGoals()
    }

    fun deleteGoal(goal: Goal) {
        dbHelper.deleteGoal(email, goal.category)
        goals.remove(goal)
    }

    fun getCategoryExpense(category: String): Double {
        return expenses[category] ?: 0.0
    }

    fun loadExpensesForPeriod(period: String) {
        _selectedPeriod = period

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val endDate = dateFormat.format(calendar.time)

        when (period) {
            "month" -> calendar.add(Calendar.MONTH, -1)
            "quarter" -> calendar.add(Calendar.MONTH, -3)
            "halfyear" -> calendar.add(Calendar.MONTH, -6)
            "year" -> calendar.add(Calendar.YEAR, -1)
        }
        val startDate = dateFormat.format(calendar.time)

        // Получаем сырые данные для графика
        expensesRaw = dbHelper.getTransactionsByPeriod(email, startDate, endDate)

        // Группируем по категориям для круговой диаграммы
        expenses = expensesRaw
            .filter { it["type"] == "expense" }
            .groupBy { it["category"] as String }
            .mapValues { it.value.sumOf { t -> t["amount"] as Double } }
    }

    fun getFormattedPeriod(period: String): String {
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        return when (period) {
            "week" -> {
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                val start = dateFormat.format(calendar.time)
                calendar.add(Calendar.DAY_OF_WEEK, 6)
                val end = dateFormat.format(calendar.time)
                "$start - $end"
            }
            "month" -> {
                val firstDay = "01 ${getCurrentMonthShort()}"
                val lastDay = "${getDaysInMonth()} ${getCurrentMonthShort()} ${getCurrentYear()}"
                "$firstDay - $lastDay"
            }
            "quarter" -> {
                val month = calendar.get(Calendar.MONTH)
                val quarterStartMonth = month - (month % 3)
                calendar.set(Calendar.MONTH, quarterStartMonth)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val start = dateFormat.format(calendar.time)

                calendar.add(Calendar.MONTH, 2)
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                val end = dateFormat.format(calendar.time)

                "$start - $end"
            }
            "halfyear" -> {
                val month = calendar.get(Calendar.MONTH)
                val halfYearStartMonth = if (month < 6) 0 else 6
                calendar.set(Calendar.MONTH, halfYearStartMonth)
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                val start = dateFormat.format(calendar.time)

                calendar.add(Calendar.MONTH, 5)
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                val end = dateFormat.format(calendar.time)

                "$start - $end"
            }
            "year" -> {
                "01 Jan ${getCurrentYear()} - 31 Dec ${getCurrentYear()}"
            }
            else -> {
                "All period"
            }
        }
    }

    private fun getDaysInMonth(): Int {
        val calendar = Calendar.getInstance()
        return calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    }

    private fun getCurrentYear(): String {
        val dateFormat = SimpleDateFormat("yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }

    private fun getCurrentMonthShort(): String {
        val dateFormat = SimpleDateFormat("MMM", Locale.getDefault())
        return dateFormat.format(Date())
    }
}