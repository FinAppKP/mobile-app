package com.example.finapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private lateinit var tvBalance: TextView
    private lateinit var layoutTransactions: LinearLayout
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        tvBalance = findViewById(R.id.tvBalance)
        layoutTransactions = findViewById(R.id.layoutTransactions)

        val btnAddTransaction = findViewById<Button>(R.id.btnAddTransaction)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        sessionManager = SessionManager(this)

        btnAddTransaction.setOnClickListener {
            startActivity(Intent(this, TransactionActivity::class.java))
        }

        btnLogout.setOnClickListener {
            sessionManager.logout()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadTransactions()
    }

    fun formatTimestamp(timestamp: String): String {
        return try {
            val millis = timestamp.toLong()
            val date = Date(millis)
            val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            format.format(date)
        } catch (e: Exception) {
            timestamp
        }
    }
    private fun loadTransactions() {
        layoutTransactions.removeAllViews()
        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            Toast.makeText(this, "Ошибка сессии", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        ApiClient.getTransactions(token) { jsonString ->
            runOnUiThread {
                try {
                    val json = JSONObject(jsonString)
                    val balance = json.optDouble("balance", 0.0)
                    val transactions = json.optJSONArray("transactions") ?: JSONArray()

                    tvBalance.text = "Баланс: $balance"

                    for (i in transactions.length() - 1 downTo 0) {
                        val tx = transactions.getJSONObject(i)
                        val type = tx.optString("type", "unknown")
                        val amount = tx.optDouble("amount", 0.0)
                        val rawTime = tx.optString("timestamp", "")
                        val timestamp = formatTimestamp(rawTime)

                        val layout = LinearLayout(this)
                        layout.orientation = LinearLayout.HORIZONTAL
                        layout.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )

                        val tvText = TextView(this)
                        tvText.textSize = 18f
                        tvText.text = when (type) {
                            "income" -> "+$amount Доход"
                            "expense" -> "-$amount Расход"
                            "cash" -> "$amount Снятие наличных"
                            "transfer" -> "$amount Перевод"
                            else -> "$amount $type"
                        }
                        tvText.setTextColor(
                            when (type) {
                                "income" -> Color.parseColor("#18B26B")
                                "expense" -> Color.parseColor("#E64545")
                                "cash" -> Color.parseColor("#7A4DFF")
                                "transfer" -> Color.parseColor("#3D6CFF")
                                else -> Color.BLACK
                            }
                        )
                        tvText.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

                        val tvTime = TextView(this)
                        tvTime.textSize = 14f
                        tvTime.setTextColor(Color.GRAY)
                        tvTime.text = timestamp
                        tvTime.layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        )

                        layout.addView(tvText)
                        layout.addView(tvTime)
                        layout.setPadding(0, 12, 0, 12)

                        layoutTransactions.addView(layout)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Ошибка при загрузке транзакций", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}