package com.example.mfl

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.mfl.Goal


class DbHelper(context: Context) : SQLiteOpenHelper(context, "MFL", null, 8) {

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL("""
        CREATE TABLE userInfo (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            email TEXT NOT NULL UNIQUE,
            cash REAL NOT NULL,
            ewallet REAL NOT NULL,
            income REAL DEFAULT 0,
            total_expenses REAL DEFAULT 0
        )
    """.trimIndent())

        db?.execSQL("""
            CREATE TABLE transactions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                userEmail TEXT NOT NULL,
                type TEXT NOT NULL, -- income / expense
                category TEXT NOT NULL,
                amount REAL NOT NULL,
                date TEXT NOT NULL,
                note TEXT
            )
        """.trimIndent())

        db?.execSQL("""
            CREATE TABLE goals (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                userEmail TEXT NOT NULL,
                category TEXT NOT NULL,
                expenseLimit REAL NOT NULL,
                period TEXT NOT NULL -- month / quarter / halfyear / year
            )
        """.trimIndent())

        db?.execSQL("""
            CREATE TABLE debts (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                userEmail TEXT NOT NULL,
                debtorName TEXT NOT NULL,
                amount REAL NOT NULL,
                date TEXT NOT NULL,
                isPaid INTEGER DEFAULT 0
            )
        """.trimIndent())

        db?.execSQL("""
            CREATE TABLE category_expenses (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                userEmail TEXT NOT NULL,
                category TEXT NOT NULL,
                period TEXT NOT NULL, -- формат: YYYY-MM
                amount REAL NOT NULL,
                UNIQUE(userEmail, category, period)
            )
        """.trimIndent())

    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS userInfo")
        db?.execSQL("DROP TABLE IF EXISTS transactions")
        db?.execSQL("DROP TABLE IF EXISTS goals")
        db?.execSQL("DROP TABLE IF EXISTS debts")
        db?.execSQL("DROP TABLE IF EXISTS category_expenses")
        onCreate(db)
    }
    fun updateCategoryExpense(email: String, category: String, period: String, amount: Double) {
        val db = writableDatabase

        val cursor = db.rawQuery("""
        SELECT amount FROM category_expenses 
        WHERE userEmail = ? AND category = ? AND period = ?
    """, arrayOf(email, category, period))

        val values = ContentValues().apply {
            put("userEmail", email)
            put("category", category)
            put("period", period)
        }

        if (cursor.moveToFirst()) {
            val currentAmount = cursor.getDouble(0)
            values.put("amount", currentAmount + amount)
            db.update("category_expenses", values, "userEmail = ? AND category = ? AND period = ?", arrayOf(email, category, period))
        } else {
            values.put("amount", amount)
            db.insert("category_expenses", null, values)
        }

        cursor.close()

    }
    fun getCategoryExpense(email: String, category: String, period: String): Double {
        val db = readableDatabase
        val cursor = db.rawQuery("""
        SELECT amount FROM category_expenses 
        WHERE userEmail = ? AND category = ? AND period = ?
    """, arrayOf(email, category, period))

        val result = if (cursor.moveToFirst()) cursor.getDouble(0) else 0.0
        cursor.close()
        return result
    }

    // ========== UserInfo ==========
    fun addOrUpdateUser(email: String, cash: Double, ewallet: Double) {
        val db = writableDatabase
        val cursor = db.rawQuery("SELECT * FROM userInfo WHERE email = ?", arrayOf(email))
        val values = ContentValues().apply {
            put("email", email)
            put("cash", cash)
            put("ewallet", ewallet)
        }

        if (cursor.moveToFirst()) {
            db.update("userInfo", values, "email = ?", arrayOf(email))
        } else {
            db.insert("userInfo", null, values)
        }

        cursor.close()
        db.close()
    }

    fun getUserBalance(email: String): Pair<Double, Double>? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT cash, ewallet FROM userInfo WHERE email = ?", arrayOf(email))
        return if (cursor.moveToFirst()) {
            val cash = cursor.getDouble(0)
            val ewallet = cursor.getDouble(1)
            cursor.close()
            Pair(cash, ewallet)
        } else {
            cursor.close()
            null
        }
    }

    // ========== Transactions ==========
    fun addTransaction(email: String, type: String, category: String, amount: Double, date: String, note: String?) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("userEmail", email)
            put("type", type)
            put("category", category)
            put("amount", amount)
            put("date", date)
            put("note", note)
        }

        db.insert("transactions", null, values)
        db.close()
    }

    fun getTransactionsByPeriod(email: String, startDate: String, endDate: String): List<Map<String, Any>> {
        val db = readableDatabase
        val list = mutableListOf<Map<String, Any>>()
        val cursor = db.rawQuery("""
            SELECT * FROM transactions 
            WHERE userEmail = ? AND date BETWEEN ? AND ?
        """, arrayOf(email, startDate, endDate))

        while (cursor.moveToNext()) {
            list.add(
                mapOf(
                    "type" to cursor.getString(cursor.getColumnIndexOrThrow("type")),
                    "category" to cursor.getString(cursor.getColumnIndexOrThrow("category")),
                    "amount" to cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                    "date" to cursor.getString(cursor.getColumnIndexOrThrow("date")),
                    "note" to cursor.getString(cursor.getColumnIndexOrThrow("note"))
                )
            )
        }

        cursor.close()
        return list
    }

    // ========== Goals ==========

    fun setMonthlyGoal(email: String, limit: Double) {
        val db = writableDatabase
        // Удаляем старую цель если была
        db.delete("goals", "userEmail = ?", arrayOf(email))
        // Добавляем новую
        val values = ContentValues().apply {
            put("userEmail", email)
            put("category", "MONTHLY_TOTAL") // Специальная категория для общего лимита
            put("expenseLimit", limit)
            put("period", "month")
        }
        db.insert("goals", null, values)
        db.close()
    }

    fun getMonthlyGoal(email: String): Double {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT expenseLimit FROM goals WHERE userEmail = ? AND category = 'MONTHLY_TOTAL'",
            arrayOf(email)
        )
        return if (cursor.moveToFirst()) cursor.getDouble(0) else 0.0.also { cursor.close() }
    }
    fun getGoals(email: String): List<Goal> {
        val db = readableDatabase
        val goals = mutableListOf<Goal>()
        val cursor = db.rawQuery("SELECT category, expenseLimit, period FROM goals WHERE userEmail = ?", arrayOf(email))

        while (cursor.moveToNext()) {
            goals.add(
                Goal(
                    category = cursor.getString(0),
                    limit = cursor.getDouble(1),
                    period = cursor.getString(2)
                )
            )
        }
        cursor.close()
        return goals
    }

    fun deleteGoal(email: String, category: String) {
        val db = writableDatabase
        db.delete("goals", "userEmail = ? AND category = ?", arrayOf(email, category))
        db.close()
    }
    fun setGoal(email: String, category: String, expenseLimit: Double, period: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("userEmail", email)
            put("category", category)
            put("expenseLimit", expenseLimit)
            put("period", period)
        }
        db.insert("goals", null, values)
        db.close()
    }


    // ========== Debts ==========
    fun addDebt(email: String, debtorName: String, amount: Double, date: String) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("userEmail", email)
            put("debtorName", debtorName)
            put("amount", amount)
            put("date", date)
        }
        db.insert("debts", null, values)
        db.close()
    }

    fun markDebtAsPaid(debtId: Int) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("isPaid", 1)
        }
        db.update("debts", values, "id = ?", arrayOf(debtId.toString()))
        db.close()
    }

    fun getDebts(email: String): List<Map<String, Any>> {
        val db = readableDatabase
        val list = mutableListOf<Map<String, Any>>()
        val cursor = db.rawQuery("SELECT * FROM debts WHERE userEmail = ? AND isPaid = 0", arrayOf(email))

        while (cursor.moveToNext()) {
            list.add(
                mapOf(
                    "id" to cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    "debtorName" to cursor.getString(cursor.getColumnIndexOrThrow("debtorName")),
                    "amount" to cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                    "date" to cursor.getString(cursor.getColumnIndexOrThrow("date"))
                )
            )
        }

        cursor.close()
        return list
    }


    // Добавить в класс DbHelper
    fun updateIncome(email: String, amount: Double) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("income", amount)
        }

        // Проверяем, есть ли уже запись для этого пользователя
        val cursor = db.rawQuery("SELECT * FROM userInfo WHERE email = ?", arrayOf(email))
        if (cursor.moveToFirst()) {
            db.update("userInfo", values, "email = ?", arrayOf(email))
        } else {
            values.put("email", email)
            values.put("cash", 0.0)
            values.put("ewallet", 0.0)
            db.insert("userInfo", null, values)
        }
        cursor.close()
    }

    fun getIncomeAmount(email: String): Double {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT income FROM userInfo WHERE email = ?", arrayOf(email))
        return if (cursor.moveToFirst()) cursor.getDouble(0) else 0.0
    }

    fun updateTotalExpenses(email: String, amount: Double) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("total_expenses", amount)
        }
        db.update("userInfo", values, "email = ?", arrayOf(email))
    }

    fun getTotalExpenses(email: String): Double {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT total_expenses FROM userInfo WHERE email = ?", arrayOf(email))
        return if (cursor.moveToFirst()) cursor.getDouble(0) else 0.0
    }

    // Add these methods to your DbHelper class
    fun getUserFullBalance(email: String): Triple<Double, Double, Double>? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT cash, ewallet, income, total_expenses FROM userInfo WHERE email = ?", arrayOf(email))
        return if (cursor.moveToFirst()) {
            Triple(
                cursor.getDouble(0),  // cash
                cursor.getDouble(1),  // ewallet
                cursor.getDouble(2)   // income
            )
        } else {
            null
        }
    }
// Добавьте эти методы в класс DbHelper

    fun getAllTransactions(email: String): List<Transaction> {
        val db = readableDatabase
        val list = mutableListOf<Transaction>()
        val cursor = db.rawQuery("SELECT * FROM transactions WHERE userEmail = ?", arrayOf(email))

        while (cursor.moveToNext()) {
            list.add(
                Transaction(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                    userEmail = cursor.getString(cursor.getColumnIndexOrThrow("userEmail")),
                    type = cursor.getString(cursor.getColumnIndexOrThrow("type")),
                    category = cursor.getString(cursor.getColumnIndexOrThrow("category")),
                    amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount")),
                    date = cursor.getString(cursor.getColumnIndexOrThrow("date")),
                    note = cursor.getString(cursor.getColumnIndexOrThrow("note"))
                )
            )
        }

        cursor.close()
        return list
    }

    data class Transaction(
        val id: Int,
        val userEmail: String,
        val type: String,
        val category: String,
        val amount: Double,
        val date: String,
        val note: String?
    )



    data class Debt(
        val id: Int,
        val userEmail: String,
        val debtorName: String,
        val amount: Double,
        val date: String,
        val isPaid: Int
    )

}
