package com.example.mfl.utils

import java.text.SimpleDateFormat
import java.util.*

fun getPeriodDates(period: String): Pair<String, String> {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val calendar = Calendar.getInstance()
    val endDate = sdf.format(calendar.time)

    when (period.lowercase()) {
        "месяц" -> calendar.add(Calendar.MONTH, -1)
        "квартал" -> calendar.add(Calendar.MONTH, -3)
        "полгода" -> calendar.add(Calendar.MONTH, -6)
        "год" -> calendar.add(Calendar.YEAR, -1)
    }

    val startDate = sdf.format(calendar.time)
    return Pair(startDate, endDate)
}
