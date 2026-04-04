package com.example.finapp

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class TransactionActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private var selectedType: String? = null
    private var selectedCategoryId: Int? = null
    private val categories = mutableListOf<ApiClient.Category>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transaction)

        sessionManager = SessionManager(this)

        val btnExpense = findViewById<Button>(R.id.btnExpense)
        val btnIncome = findViewById<Button>(R.id.btnIncome)
        val layoutPayment = findViewById<LinearLayout>(R.id.layoutPaymentMethod)
        val btnCash = findViewById<Button>(R.id.btnCash)
        val btnTransfer = findViewById<Button>(R.id.btnTransfer)
        val etAmount = findViewById<EditText>(R.id.etAmount)
        val radioGroupPayment = findViewById<RadioGroup>(R.id.radioGroupPayment)
        val btnSave = findViewById<Button>(R.id.btnSaveTransaction)
        val spinnerCategory = findViewById<Spinner>(R.id.spinnerCategory)
        val btnAddCategory = findViewById<Button>(R.id.btnAddCategory)

        selectedType = null

        // --- Тип транзакции ---
        btnIncome.setOnClickListener {
            layoutPayment.visibility = View.GONE
            selectedType = "income"
            loadCategories(sessionManager.getToken(), "income", spinnerCategory)
        }

        btnExpense.setOnClickListener {
            layoutPayment.visibility = View.VISIBLE
            selectedType = "expense"
            loadCategories(sessionManager.getToken(), "expense", spinnerCategory)
        }

        btnCash.setOnClickListener {
            layoutPayment.visibility = View.GONE
            selectedType = "cash"
            loadCategories(sessionManager.getToken(), null, spinnerCategory)
        }

        btnTransfer.setOnClickListener {
            layoutPayment.visibility = View.GONE
            selectedType = "transfer"
            loadCategories(sessionManager.getToken(), null, spinnerCategory)
        }

// --- Добавить новую категорию ---
        btnAddCategory.setOnClickListener {
            val input = EditText(this)
            AlertDialog.Builder(this)
                .setTitle("Новая категория")
                .setView(input)
                .setPositiveButton("Сохранить") { _, _ ->
                    val name = input.text.toString().trim()
                    val type = selectedType
                    if (name.isNotEmpty() && type != null) {
                        val token = sessionManager.getToken()
                        if (token.isNullOrEmpty()) return@setPositiveButton

                        // POST-запрос на сервер
                        ApiClient.createCategory(token, name, type) { newCat ->
                            runOnUiThread {
                                if (newCat != null) {
                                    categories.add(newCat)
                                    updateSpinner(spinnerCategory)
                                    selectedCategoryId = newCat.id
                                    Toast.makeText(this, "Категория добавлена", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this, "Ошибка при добавлении категории", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
                .setNegativeButton("Отмена", null)
                .show()
        }

        // --- Сохранение транзакции ---
        btnSave.setOnClickListener {
            val token = sessionManager.getToken()
            if (token.isNullOrEmpty()) {
                Toast.makeText(this, "Ошибка сессии", Toast.LENGTH_SHORT).show()
                finish()
                return@setOnClickListener
            }

            val amountText = etAmount.text.toString().trim()
            val normalized = amountText.replace(",", ".")
            val amount = normalized.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Введите корректную сумму", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val type = selectedType ?: run {
                Toast.makeText(this, "Выберите тип операции", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val paymentMethodId = radioGroupPayment.checkedRadioButtonId
            val paymentMethodText = if (paymentMethodId != -1) findViewById<RadioButton>(paymentMethodId).text.toString() else ""
            val paymentMethod = when (paymentMethodText) {
                "Наличные" -> "cash"
                "Безналичные" -> "card"
                else -> ""
            }

            if (type == "expense" && paymentMethod.isEmpty()) {
                Toast.makeText(this, "Выберите способ оплаты", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSave.isEnabled = false

            ApiClient.createTransaction(token, type, amount, paymentMethod, selectedCategoryId) { success ->
                runOnUiThread {
                    btnSave.isEnabled = true
                    if (success) {
                        Toast.makeText(this, "Транзакция добавлена", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Ошибка сети или сервера", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun loadCategories(token: String?, typeFilter: String?, spinner: Spinner) {
        if (token.isNullOrEmpty()) return

        ApiClient.getCategories(token) { list ->
            categories.clear()
            if (typeFilter != null) {
                categories.addAll(list.filter { it.type == typeFilter })
            } else {
                categories.addAll(list)
            }
            runOnUiThread { updateSpinner(spinner) }
        }
    }

    private fun updateSpinner(spinner: Spinner) {
        val names = categories.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategoryId = categories[position].id
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedCategoryId = null
            }
        }
    }
}