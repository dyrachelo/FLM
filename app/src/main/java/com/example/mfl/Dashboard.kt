package com.example.mfl

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.ClipData
import android.view.DragEvent
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import android.util.Log



class Dashboard : Fragment() {
    private var walletBalance = 0.0
    private var foodExpense = 0.0
    private var houseExpense = 0.0
    private var clothesExpense = 0.0
    private var communicationsExpense = 0.0
    private var entertainmentExpense = 0.0
    private var groceriesExpense = 0.0
    private var pocketExpense = 0.0
    private var transportExpense = 0.0
    private var travelExpense = 0.0

    private var eWalletBalance = 0.0

    private lateinit var dbHelper: DbHelper
    private lateinit var email: String
    private lateinit var walletTextView: TextView
    private lateinit var eWalletTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = DbHelper(requireContext())
        val rawEmail = requireActivity().intent.getStringExtra("userEmail")
        Log.d("DashboardFragment", "Retrieved email from intent: $rawEmail")

        email = rawEmail.toString() // Будь осторожен: .toString() вернёт "null" как строку, если null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        walletTextView = view.findViewById(R.id.wallet_cash)
        eWalletTextView = view.findViewById(R.id.eWallet)

        val imgWallet = view.findViewById<ImageView>(R.id.wallet)
        val imgEWallet = view.findViewById<ImageView>(R.id.eWalletImg)
        val imgFood = view.findViewById<ImageView>(R.id.foodExpense)
        val imgHouse = view.findViewById<ImageView>(R.id.HouseExpense)
        val imgClothes = view.findViewById<ImageView>(R.id.ClothesExpense)
        val imgCommunications = view.findViewById<ImageView>(R.id.CommunicationsExpense)
        val imgEntertainment = view.findViewById<ImageView>(R.id.EntertainmentExpense)
        val imgGroceries = view.findViewById<ImageView>(R.id.GroceriesExpense)
        val imgPocket = view.findViewById<ImageView>(R.id.PocketExpense)
        val imgTransport = view.findViewById<ImageView>(R.id.TransportExpense)
        val imgTravel = view.findViewById<ImageView>(R.id.TravelExpense)

        val wallet = view.findViewById<TextView>(R.id.wallet_cash)
        val income = view.findViewById<TextView>(R.id.income)
        val eWallet = view.findViewById<TextView>(R.id.eWallet)
        val textFood = view.findViewById<TextView>(R.id.foodExpenseText)
        val textHouse = view.findViewById<TextView>(R.id.HouseExpenseText)
        val textClothes = view.findViewById<TextView>(R.id.ClothesExpenseText)
        val textCommunications = view.findViewById<TextView>(R.id.CommunicationsExpenseText)
        val textEntertainment = view.findViewById<TextView>(R.id.EntertainmentExpenseText)
        val textGroceries = view.findViewById<TextView>(R.id.GroceriesExpenseText)
        val textPocket = view.findViewById<TextView>(R.id.PocketExpenseText)
        val textTransport = view.findViewById<TextView>(R.id.TransportExpenseText)
        val textTravel = view.findViewById<TextView>(R.id.TravelExpenseText)

        // Загрузка баланса пользователя
        val balance = dbHelper.getUserBalance(email)
        if (balance != null) {
            walletBalance = balance.first
            eWalletBalance = balance.second
        }

        // Загружаем расходы по категориям
        loadExpenses()

        // Обновляем UI
        walletTextView.text = "$walletBalance Br"
        eWalletTextView.text = "$eWalletBalance Br"
        textFood.text = "$foodExpense Br"
        textHouse.text = "$houseExpense Br"
        textClothes.text = "$clothesExpense Br"
        textCommunications.text = "$communicationsExpense Br"
        textEntertainment.text = "$entertainmentExpense Br"
        textGroceries.text = "$groceriesExpense Br"
        textPocket.text = "$pocketExpense Br"
        textTransport.text = "$transportExpense Br"
        textTravel.text = "$travelExpense Br"

        // Обработчики нажатий
        imgWallet.setOnLongClickListener {
            val clipData = ClipData.newPlainText("sourceWallet", "Wallet")
            val shadowBuilder = View.DragShadowBuilder(it)
            it.startDragAndDrop(clipData, shadowBuilder, it, 0)
            true
        }

        imgEWallet.setOnLongClickListener {
            val clipData = ClipData.newPlainText("sourceWallet", "eWallet")
            val shadowBuilder = View.DragShadowBuilder(it)
            it.startDragAndDrop(clipData, shadowBuilder, it, 0)
            true
        }

        imgWallet.setOnClickListener {
            showAmountInputDialog("Wallet", "wallet", wallet)
        }

        imgEWallet.setOnClickListener {
            showAmountInputDialog("eWallet", "eWallet", eWallet)
        }

        income.setOnClickListener {
            showAmountInputDialog("Income", "income", income)
        }

        setupDropTarget(imgFood, "Food")
        setupDropTarget(imgHouse, "House")
        setupDropTarget(imgClothes, "Clothes")
        setupDropTarget(imgCommunications, "Communications")
        setupDropTarget(imgEntertainment, "Entertainment")
        setupDropTarget(imgGroceries, "Groceries")
        setupDropTarget(imgPocket, "Pocket")
        setupDropTarget(imgTransport, "Transport")
        setupDropTarget(imgTravel, "Travel")

        return view
    }

    private fun setupDropTarget(target: ImageView, category: String) {
        target.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DROP -> {
                    val sourceWallet = event.clipData.getItemAt(0).text.toString().lowercase()                    // Вызов диалога для ввода суммы
                    showExpenseInputDialog(category, sourceWallet)
                    true
                }
                else -> true
            }
        }
    }

    // Метод для изменения баланса без проверки на достаточность средств
    private fun showAmountInputDialog(category: String, sourceWallet: String, textView: TextView) {
        val input = EditText(requireContext())
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Enter Amount")
            .setMessage("How much would you like to set for $category?")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val amountStr = input.text.toString()
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                if (amount >= 0) {
                    when (sourceWallet) {
                        "wallet" -> {
                            walletBalance = amount
                            textView.text = "$walletBalance Br"
                        }
                        "eWallet" -> {
                            eWalletBalance = amount
                            textView.text = "$eWalletBalance Br"
                        }
                        "income" -> {
                            walletBalance += amount
                            textView.text = "$walletBalance Br"
                        }
                    }
                    dbHelper.addOrUpdateUser(email, walletBalance, eWalletBalance)
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun showExpenseInputDialog(category: String, sourceWallet: String) {
        val input = EditText(requireContext())
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Enter Expense Amount")
            .setMessage("How much did you spend on $category?")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val amountStr = input.text.toString()
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                if (amount > 0) {
                    val wallet = sourceWallet.lowercase() // нормализуем

                    if (wallet == "wallet" && amount <= walletBalance) {
                        walletBalance -= amount
                        val date = Calendar.getInstance().time.toString()
                        val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                        val period = dateFormat.format(Calendar.getInstance().time)

                        dbHelper.addTransaction(email, "expense", category, amount, date, null)
                        dbHelper.updateCategoryExpense(email, category, period, amount)
                    } else if (wallet == "ewallet" && amount <= eWalletBalance) {
                        eWalletBalance -= amount
                        val date = Calendar.getInstance().time.toString()
                        val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                        val period = dateFormat.format(Calendar.getInstance().time)

                        dbHelper.addTransaction(email, "expense", category, amount, date, null)
                        dbHelper.updateCategoryExpense(email, category, period, amount)
                    }

                    else {
                        showToast("Insufficient balance")
                        return@setPositiveButton
                    }

                    dbHelper.addOrUpdateUser(email, walletBalance, eWalletBalance)
                    refreshDashboardData()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }
    private fun refreshDashboardData() {
        loadExpenses()
        updateBalances()
        updateExpenseTexts()
    }

    private fun updateBalances() {
        walletTextView.text = "$walletBalance Br"
        eWalletTextView.text = "$eWalletBalance Br"
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    private fun updateExpenseTexts() {
        view?.findViewById<TextView>(R.id.foodExpenseText)?.text = "$foodExpense Br"
        view?.findViewById<TextView>(R.id.HouseExpenseText)?.text = "$houseExpense Br"
        view?.findViewById<TextView>(R.id.ClothesExpenseText)?.text = "$clothesExpense Br"
        view?.findViewById<TextView>(R.id.CommunicationsExpenseText)?.text = "$communicationsExpense Br"
        view?.findViewById<TextView>(R.id.EntertainmentExpenseText)?.text = "$entertainmentExpense Br"
        view?.findViewById<TextView>(R.id.GroceriesExpenseText)?.text = "$groceriesExpense Br"
        view?.findViewById<TextView>(R.id.PocketExpenseText)?.text = "$pocketExpense Br"
        view?.findViewById<TextView>(R.id.TransportExpenseText)?.text = "$transportExpense Br"
        view?.findViewById<TextView>(R.id.TravelExpenseText)?.text = "$travelExpense Br"
    }

    private fun loadExpenses() {
        val dateFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val period = dateFormat.format(Calendar.getInstance().time)

        // Обнуляем
        foodExpense = dbHelper.getCategoryExpense(email, "Food", period)
        houseExpense = dbHelper.getCategoryExpense(email, "House", period)
        clothesExpense = dbHelper.getCategoryExpense(email, "Clothes", period)
        communicationsExpense = dbHelper.getCategoryExpense(email, "Communications", period)
        entertainmentExpense = dbHelper.getCategoryExpense(email, "Entertainment", period)
        groceriesExpense = dbHelper.getCategoryExpense(email, "Groceries", period)
        pocketExpense = dbHelper.getCategoryExpense(email, "Pocket", period)
        transportExpense = dbHelper.getCategoryExpense(email, "Transport", period)
        travelExpense = dbHelper.getCategoryExpense(email, "Travel", period)
    }

}
