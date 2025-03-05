package com.example.mfl

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DbHelper(val context: Context, val factory: SQLiteDatabase.CursorFactory?): SQLiteOpenHelper(context, "MFL", factory, 1) {
    override fun onCreate(db: SQLiteDatabase?) {
        val query = "Create TABLE users (id INTEGER PRIMARY KEY AUTOINCREMENT, login TEXT NOT NULL, email TEXT NOT NULL UNIQUE, pass TEXT NOT NULL)"
        db!!.execSQL(query)


    }

    override fun onUpgrade(db: SQLiteDatabase?, p1: Int, p2: Int) {
        db!!.execSQL("DROP TABLE IF EXISTS users")
        onCreate(db)
    }

    fun addUser(user: User){
        val values = ContentValues()
        values.put("login", user.login)
        values.put("email", user.email)
        values.put("pass", user.pass)

        val db = this.writableDatabase
        db.insert("users", null, values)
        db.close()
    }

    fun getUser(login: String, pass: String):Boolean{
        val db = this.readableDatabase
        val result  = db.rawQuery("SELECT * FROM users WHERE login = '$login' AND pass = '$pass'", null)

        return result.moveToFirst()
    }

}