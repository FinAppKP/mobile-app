package com.example.finapp

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context) :
    SQLiteOpenHelper(context, "users.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {

        val createUsersTable = """
            CREATE TABLE users(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            email TEXT,
            password TEXT
            )
        """.trimIndent()

        val createTransactionsTable = """
            CREATE TABLE transactions(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            email TEXT,
            type TEXT,
            amount REAL,
            payment_method TEXT,
            timestamp TEXT
            )
        """.trimIndent()

        db.execSQL(createUsersTable)
        db.execSQL(createTransactionsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    // Регистрация пользователя
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

    // Проверка логина
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

    // Сохранение транзакции
    fun insertTransaction(
        email: String,
        type: String,
        amount: Double,
        paymentMethod: String,
        timestamp: String
    ): Boolean {

        val db = writableDatabase
        val values = ContentValues()

        values.put("email", email)
        values.put("type", type)
        values.put("amount", amount)
        values.put("payment_method", paymentMethod)
        values.put("timestamp", timestamp)

        val result = db.insert("transactions", null, values)

        return result != -1L
    }

    // Получение всех транзакций пользователя
    fun getUserTransactions(email: String): List<Map<String, String>> {

        val transactions = mutableListOf<Map<String, String>>()

        val db = readableDatabase

        val cursor = db.rawQuery(
            "SELECT type, amount, timestamp FROM transactions WHERE email=? ORDER BY id DESC",
            arrayOf(email)
        )

        if (cursor.moveToFirst()) {
            do {
                val type = cursor.getString(0)
                val amount = cursor.getDouble(1)
                val timestamp = cursor.getString(cursor.getColumnIndex("timestamp"))
                val transaction = mapOf(
                    "type" to type,
                    "amount" to amount.toString(),
                    "timestamp" to timestamp
                )

                transactions.add(transaction)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return transactions
    }
}