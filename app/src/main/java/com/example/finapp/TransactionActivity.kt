package com.example.finapp

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class TransactionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction)

        val btnExpense = findViewById<Button>(R.id.btnExpense)
        val layoutPayment = findViewById<LinearLayout>(R.id.layoutPaymentMethod)

        btnExpense.setOnClickListener {
            layoutPayment.visibility = View.VISIBLE
        }

        findViewById<Button>(R.id.btnIncome).setOnClickListener {
            layoutPayment.visibility = View.GONE
        }

        findViewById<Button>(R.id.btnCash).setOnClickListener {
            layoutPayment.visibility = View.GONE
        }

        findViewById<Button>(R.id.btnTransfer).setOnClickListener {
            layoutPayment.visibility = View.GONE
        }
    }
}