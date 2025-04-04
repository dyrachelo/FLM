package com.example.mfl

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.ClipData
import android.content.DialogInterface
import android.view.DragEvent
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity


class Dashboard : Fragment() {
    private var walletBalance = 0.0
    private var foodExpense = 0.0
    private var eWalletBalance = 0.0

    private lateinit var dbHelper: DbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Инициализация dbHelper
        dbHelper = DbHelper(requireContext())

    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        val imgWallet = view.findViewById<ImageView>(R.id.wallet)
        val imgEWallet = view.findViewById<ImageView>(R.id.eWalletImg)
        val imgFood = view.findViewById<ImageView>(R.id.foodExpense)
        var wallet = view.findViewById<TextView>(R.id.wallet_cash)
        var income = view.findViewById<TextView>(R.id.income)
        var eWallet = view.findViewById<TextView>(R.id.eWallet)
        var textFood = view.findViewById<TextView>(R.id.foodExpenseText)

        val email = requireActivity().intent.getStringExtra("userEmail").toString()
        val userInfo = dbHelper.getUserInfo(email)
        if (userInfo != null) {
            val (loadedWalletBalance, loadedEwallet, loadedFoodExpense) = userInfo // Распаковка значений
            walletBalance = loadedWalletBalance
            foodExpense = loadedFoodExpense
            eWalletBalance = loadedEwallet
        }

        wallet.text = walletBalance.toString() + " $"
        //income.text = income.toString() + " $"
        eWallet.text = eWalletBalance.toString() + " $"
        textFood.text = foodExpense.toString() + " $"

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
            showAmountInputDialog("Wallet", wallet)
        }

        imgEWallet.setOnClickListener {
            showAmountInputDialog("eWallet", eWallet)
        }

        income.setOnClickListener {
            showAmountInputDialog("Income", income)
        }



        setupDropTarget(imgFood, "Food", "Wallet")
        setupDropTarget(imgFood, "Food", "eWallet")
//        setupDropTarget(imgTransport, "Transport")
//        setupDropTarget(imgEntertainment, "Entertainment")

        return view
    }



    private fun setupDropTarget(target: ImageView, category: String, sourceWallet: String) {
        target.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DROP -> {
                    val sourceWallet = event.clipData.getItemAt(0).text.toString()
                    showAmountInputDialog(category, sourceWallet)
                    true
                }
                else -> true
            }
        }
    }

    private fun showAmountInputDialog(category: String, sourceWallet: String) {
        val input = EditText(requireContext())
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Enter Amount")
            .setMessage("How much did you spend on $category?")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val amountStr = input.text.toString()
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                if (amount > 0) {
                    if (sourceWallet == "Wallet" && amount <= walletBalance) {
                        walletBalance -= amount
                    } else if (sourceWallet == "eWallet" && amount <= eWalletBalance) {
                        eWalletBalance -= amount
                    } else {
                        showToast("Invalid amount or insufficient balance")
                        return@setPositiveButton
                    }

                    when (category) {
                        "Food" -> foodExpense += amount
                    }

                    // Обновляем UI
                    view?.findViewById<TextView>(R.id.wallet_cash)?.text = "$walletBalance $"
                    view?.findViewById<TextView>(R.id.eWallet)?.text = "$eWalletBalance $"
                    view?.findViewById<TextView>(R.id.foodExpenseText)?.text = "$foodExpense $"

                    // Сохраняем данные
                    val email = requireActivity().intent.getStringExtra("userEmail").toString()
                    val updatedUserInfo = UserInfo(email, walletBalance, eWalletBalance, foodExpense)
                    dbHelper.addUserInfo(updatedUserInfo)

                    showToast("Spent $amount on $category\nWallet: $walletBalance Br\nE-Wallet: $eWalletBalance Br")
                } else {
                    showToast("Invalid amount")
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }



    private fun showAmountInputDialog(field: String, textView: TextView) {
        val input = EditText(requireContext())
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Enter Amount")
            .setMessage("Enter amount for $field")
            .setView(input)
            .setPositiveButton("OK") { _, _ ->
                val amountStr = input.text.toString()
                val amount = amountStr.toDoubleOrNull() ?: 0.0
                if (amount > 0) {
                    when (field) {
                        "Wallet" -> walletBalance += amount
                        "eWallet" -> eWalletBalance += amount
                        "Income" -> {
                            // Например, можно добавить к кошельку доход
                            walletBalance += amount
                        }
                    }

                    // Сохраняем обновленные данные
                    val email = requireActivity().intent.getStringExtra("userEmail").toString()
                    val updatedUserInfo = UserInfo(email, walletBalance, eWalletBalance, foodExpense)
                    dbHelper.addUserInfo(updatedUserInfo)

                    // Обновляем UI
                    textView.text = "$amount $"

                    showToast("Updated $field: $amount $")
                } else {
                    showToast("Invalid amount")
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }


    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
}