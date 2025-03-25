package com.example.mfl

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DbHelper(context: Context) : SQLiteOpenHelper(context, "MFL", null, 2)
{    override fun onCreate(db: SQLiteDatabase?) {
        val query = """
            CREATE TABLE userInfo (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                email TEXT NOT NULL UNIQUE,
                cash REAL NOT NULL,
                ewallet REAL NOT NULL,
                foodExpense REAL NOT NULL
            )
        """.trimIndent()
        db?.execSQL(query)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS userInfo")
        onCreate(db)
    }

    fun addUserInfo(userInfo: UserInfo) {
        val db = this.writableDatabase
        val cursor = db.rawQuery("SELECT * FROM userInfo WHERE email = ?", arrayOf(userInfo.email))

        val values = ContentValues().apply {
            put("email", userInfo.email)
            put("cash", userInfo.cash)
            put("ewallet", userInfo.ewallet)
            put("foodExpense", userInfo.foodExpense)
        }

        if (cursor.moveToFirst()) {
            // Если запись существует, выполняем UPDATE
            db.update("userInfo", values, "email = ?", arrayOf(userInfo.email))
        } else {
            // Если записи нет, выполняем INSERT
            db.insert("userInfo", null, values)
        }

        cursor.close()
        db.close()
    }

    fun getUserInfo(email: String): Triple<Double, Double, Double>? {
        val db = this.readableDatabase
        val query = "SELECT cash, ewallet, foodExpense FROM userInfo WHERE email = ?"
        val result = db.rawQuery(query, arrayOf(email))

        return if (result.moveToFirst()) {
            // Получаем индексы колонок
            val cashIndex = result.getColumnIndex("cash")
            val ewalletIndex = result.getColumnIndex("ewallet")
            val foodExpenseIndex = result.getColumnIndex("foodExpense")

            // Проверяем, что индексы валидные
            if (cashIndex != -1 && ewalletIndex != -1 && foodExpenseIndex != -1) {
                val walletBalance = result.getDouble(cashIndex)
                val ewallet = result.getDouble(ewalletIndex)
                val foodExpense = result.getDouble(foodExpenseIndex)
                result.close()
                Triple(walletBalance, ewallet, foodExpense)
            } else {
                result.close()
                null
            }
        } else {
            result.close()
            null
        }
    }
}