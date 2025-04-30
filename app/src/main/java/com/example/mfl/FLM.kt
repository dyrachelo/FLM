package com.example.mfl

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

class FLM : Application() {
    override fun onCreate() {
        super.onCreate()
        // Инициализация Firebase
        FirebaseApp.initializeApp(this)
        // Включаем оффлайн-режим Firebase
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
    }
}