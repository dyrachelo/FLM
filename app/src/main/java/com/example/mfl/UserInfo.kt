package com.example.mfl

data class UserInfo(
    val email: String,
    var cash: Double = 0.0,
    var ewallet: Double = 0.0,
    var foodExpense: Double = 0.0,
    var houseExpense: Double = 0.0,
    var clothesExpense: Double = 0.0,
    var communicationsExpense: Double = 0.0,
    var entertainmentExpense: Double = 0.0,
    var groceriesExpense: Double = 0.0,
    var pocketExpense: Double = 0.0,
    var transportExpense: Double = 0.0,
    var travelExpense: Double = 0.0
)
