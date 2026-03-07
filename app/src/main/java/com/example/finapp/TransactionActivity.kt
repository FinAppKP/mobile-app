package com.example.finapp

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class TransactionActivity : AppCompatActivity() {

    private lateinit var dbHelper: DBHelper
    private lateinit var sessionManager: SessionManager
    private var selectedType: String? = null // <-- выбранный тип транзакции

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction)

        dbHelper = DBHelper(this)
        sessionManager = SessionManager(this)

        val btnExpense = findViewById<Button>(R.id.btnExpense)
        val btnIncome = findViewById<Button>(R.id.btnIncome)
        val layoutPayment = findViewById<LinearLayout>(R.id.layoutPaymentMethod)
        val btnCash = findViewById<Button>(R.id.btnCash)
        val btnTransfer = findViewById<Button>(R.id.btnTransfer)
        val etAmount = findViewById<EditText>(R.id.etAmount)
        val radioGroupPayment = findViewById<RadioGroup>(R.id.radioGroupPayment)
        val btnSave = findViewById<Button>(R.id.btnSaveTransaction)

        // Выбор типа операции
        btnIncome.setOnClickListener {
            layoutPayment.visibility = View.GONE
            selectedType = "income"
        }

        btnExpense.setOnClickListener {
            layoutPayment.visibility = View.VISIBLE
            selectedType = "expense"
        }

        btnCash.setOnClickListener {
            layoutPayment.visibility = View.GONE
            selectedType = "cash"
        }

        btnTransfer.setOnClickListener {
            layoutPayment.visibility = View.GONE
            selectedType = "transfer"
        }

        // Сохранение транзакции
        btnSave.setOnClickListener {
            val email = sessionManager.getUserEmail()
            if (email.isNullOrEmpty()) {
                Toast.makeText(this, "Ошибка сессии", Toast.LENGTH_SHORT).show()
                finish()
                return@setOnClickListener
            }

            val amountText = etAmount.text.toString().trim()
            if (amountText.isEmpty()) {
                Toast.makeText(this, "Введите сумму", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountText.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Введите корректную сумму", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val type = selectedType ?: run {
                Toast.makeText(this, "Выберите тип операции", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val paymentMethodId = radioGroupPayment.checkedRadioButtonId
            val paymentMethod = if (paymentMethodId != -1) {
                findViewById<RadioButton>(paymentMethodId).text.toString()
            } else {
                ""
            }

            val timestamp = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
                .format(Date())

            val inserted = dbHelper.insertTransaction(email, type, amount, paymentMethod, timestamp)
            if (inserted) {
                Toast.makeText(this, "Транзакция добавлена", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Ошибка при добавлении транзакции", Toast.LENGTH_SHORT).show()
            }
        }
    }
}