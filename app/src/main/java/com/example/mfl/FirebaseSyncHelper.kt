package com.example.mfl

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class FirebaseSyncHelper(private val context: Context) {
    private val dbHelper = DbHelper(context)
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val prefs = context.getSharedPreferences("SyncPrefs", Context.MODE_PRIVATE)


    fun syncAllData() {
        val user = auth.currentUser ?: return
        val userId = user.uid
        val email = user.email ?: return

        if (!isOnline()) return

        syncLocalToFirebase(userId, email)
        syncFirebaseToLocal(userId, email)
    }

    private fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun syncLocalToFirebase(userId: String, email: String) {
        // 1. Синхронизация userInfo
        val userInfo = dbHelper.getUserFullBalance(email)
        userInfo?.let { (cash, ewallet, income) ->
            val totalExpenses = dbHelper.getTotalExpenses(email)
            database.getReference("users/$userId/userInfo").setValue(
                mapOf(
                    "email" to email,
                    "cash" to cash,
                    "ewallet" to ewallet,
                    "income" to income,
                    "total_expenses" to totalExpenses
                )
            )
        }

        // 2. Синхронизация transactions
        val transactionsRef = database.getReference("users/$userId/transactions")
        val localTransactions = dbHelper.getAllTransactions(email)

        transactionsRef.removeValue() // Очищаем перед полной синхронизацией
        localTransactions.forEach { transaction ->
            transactionsRef.push().setValue(
                mapOf(
                    "userEmail" to transaction.userEmail,
                    "type" to transaction.type,
                    "category" to transaction.category,
                    "amount" to transaction.amount,
                    "date" to transaction.date,
                    "note" to (transaction.note ?: "")
                )
            )
        }

        // 3. Синхронизация goals
        val goalsRef = database.getReference("users/$userId/goals")
        val localGoals = dbHelper.getGoals(email)

        goalsRef.removeValue()
        localGoals.forEach { goal ->
            goalsRef.push().setValue(
                mapOf(
                    "userEmail" to email,
                    "category" to goal.category,
                    "expenseLimit" to goal.limit,
                    "period" to goal.period
                )
            )
        }

        // 4. Синхронизация debts
        val debtsRef = database.getReference("users/$userId/debts")
        val localDebts = dbHelper.getDebts(email).map {
            DbHelper.Debt(
                it["id"] as Int,
                it["userEmail"] as String,
                it["debtorName"] as String,
                it["amount"] as Double,
                it["date"] as String,
                it.getOrDefault("isPaid", 0) as Int
            )
        }

        debtsRef.removeValue()
        localDebts.forEach { debt ->
            debtsRef.push().setValue(
                mapOf(
                    "userEmail" to debt.userEmail,
                    "debtorName" to debt.debtorName,
                    "amount" to debt.amount,
                    "date" to debt.date,
                    "isPaid" to debt.isPaid
                )
            )
        }

        // 5. Синхронизация category_expenses
        syncCategoryExpenses(userId, email)
    }

    private fun syncCategoryExpenses(userId: String, email: String) {
        val categoryExpensesRef = database.getReference("users/$userId/category_expenses")
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM category_expenses WHERE userEmail = ?", arrayOf(email))

        categoryExpensesRef.removeValue()
        while (cursor.moveToNext()) {
            categoryExpensesRef.push().setValue(
                mapOf(
                    "userEmail" to cursor.getString(cursor.getColumnIndexOrThrow("userEmail")),
                    "category" to cursor.getString(cursor.getColumnIndexOrThrow("category")),
                    "period" to cursor.getString(cursor.getColumnIndexOrThrow("period")),
                    "amount" to cursor.getDouble(cursor.getColumnIndexOrThrow("amount"))
                )
            )
        }
        cursor.close()
    }

    private fun syncFirebaseToLocal(userId: String, email: String) {
        // 1. Синхронизация userInfo
        database.getReference("users/$userId/userInfo").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val cash = snapshot.child("cash").getValue(Double::class.java) ?: 0.0
                    val ewallet = snapshot.child("ewallet").getValue(Double::class.java) ?: 0.0
                    val income = snapshot.child("income").getValue(Double::class.java) ?: 0.0
                    val totalExpenses = snapshot.child("total_expenses").getValue(Double::class.java) ?: 0.0

                    dbHelper.addOrUpdateUser(email, cash, ewallet)
                    dbHelper.updateIncome(email, income)
                    dbHelper.updateTotalExpenses(email, totalExpenses)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseSync", "Error syncing userInfo: ${error.message}")
            }
        })

        // 2. Синхронизация transactions
        database.getReference("users/$userId/transactions").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val existingTransactions = dbHelper.getAllTransactions(email)

                for (transactionSnapshot in snapshot.children) {
                    val type = transactionSnapshot.child("type").getValue(String::class.java) ?: continue
                    val category = transactionSnapshot.child("category").getValue(String::class.java) ?: continue
                    val amount = transactionSnapshot.child("amount").getValue(Double::class.java) ?: continue
                    val date = transactionSnapshot.child("date").getValue(String::class.java) ?: continue
                    val note = transactionSnapshot.child("note").getValue(String::class.java)

                    val exists = existingTransactions.any {
                        it.type == type && it.category == category &&
                                it.amount == amount && it.date == date
                    }

                    if (!exists) {
                        dbHelper.addTransaction(email, type, category, amount, date, note)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseSync", "Error syncing transactions: ${error.message}")
            }
        })

        // 3. Синхронизация goals
        database.getReference("users/$userId/goals").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val existingGoals = dbHelper.getGoals(email)

                for (goalSnapshot in snapshot.children) {
                    val category = goalSnapshot.child("category").getValue(String::class.java) ?: continue
                    val limit = goalSnapshot.child("expenseLimit").getValue(Double::class.java) ?: continue
                    val period = goalSnapshot.child("period").getValue(String::class.java) ?: continue

                    val exists = existingGoals.any { it.category == category && it.period == period }

                    if (!exists) {
                        dbHelper.setGoal(email, category, limit, period)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseSync", "Error syncing goals: ${error.message}")
            }
        })

        // 4. Синхронизация debts
        database.getReference("users/$userId/debts").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val existingDebts = dbHelper.getDebts(email).map {
                    DbHelper.Debt(
                        it["id"] as Int,
                        it["userEmail"] as String,
                        it["debtorName"] as String,
                        it["amount"] as Double,
                        it["date"] as String,
                        it.getOrDefault("isPaid", 0) as Int
                    )
                }

                for (debtSnapshot in snapshot.children) {
                    val debtorName = debtSnapshot.child("debtorName").getValue(String::class.java) ?: continue
                    val amount = debtSnapshot.child("amount").getValue(Double::class.java) ?: continue
                    val date = debtSnapshot.child("date").getValue(String::class.java) ?: continue
                    val isPaid = debtSnapshot.child("isPaid").getValue(Int::class.java) ?: 0

                    val exists = existingDebts.any {
                        it.debtorName == debtorName &&
                                it.amount == amount &&
                                it.date == date
                    }

                    if (!exists && isPaid == 0) {
                        dbHelper.addDebt(email, debtorName, amount, date)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseSync", "Error syncing debts: ${error.message}")
            }
        })

        // 5. Синхронизация category_expenses
        database.getReference("users/$userId/category_expenses").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val db = dbHelper.writableDatabase

                for (expenseSnapshot in snapshot.children) {
                    val category = expenseSnapshot.child("category").getValue(String::class.java) ?: continue
                    val period = expenseSnapshot.child("period").getValue(String::class.java) ?: continue
                    val amount = expenseSnapshot.child("amount").getValue(Double::class.java) ?: continue

                    val values = ContentValues().apply {
                        put("userEmail", email)
                        put("category", category)
                        put("period", period)
                        put("amount", amount)
                    }

                    db.insertWithOnConflict(
                        "category_expenses",
                        null,
                        values,
                        SQLiteDatabase.CONFLICT_REPLACE
                    )
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("FirebaseSync", "Error syncing category_expenses: ${error.message}")
            }
        })
    }
}