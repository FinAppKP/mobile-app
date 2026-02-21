package com.example.finapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) :
    SQLiteOpenHelper(context, "users.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "email TEXT UNIQUE," +
                    "password TEXT)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun registerUser(email: String, password: String): Boolean {
        val db = writableDatabase
        val values = ContentValues()
        values.put("email", email)
        values.put("password", password)

        return try {
            db.insertOrThrow("users", null, values)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun loginUser(email: String, password: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM users WHERE email=? AND password=?",
            arrayOf(email, password)
        )

        val exists = cursor.count > 0
        cursor.close()
        return exists
    }
}