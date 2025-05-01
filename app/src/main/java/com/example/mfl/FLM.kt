package com.example.mfl

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.database.FirebaseDatabase

class FLM : Application() {
    override fun onCreate() {
        super.onCreate()

        // 1. Инициализация Firebase
        FirebaseApp.initializeApp(this)

        // 2. Настройка базы данных
        val database = FirebaseDatabase.getInstance()

        // 3. Включение оффлайн-режима (должно быть перед любым использованием БД)
        database.setPersistenceEnabled(true)

        // 4. Рекомендуется также установить кэширование
        database.setPersistenceCacheSizeBytes(10_000_000) // 10MB кэша
    }
}