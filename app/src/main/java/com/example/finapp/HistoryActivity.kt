package com.example.finapp

import android.app.Dialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var list: LinearLayout
    private lateinit var etDay: EditText
    private lateinit var monthFilterButton: TextView
    private lateinit var categoryFilterButton: TextView
    private lateinit var totalText: TextView

    private var allTransactions: List<JSONObject> = emptyList()
    private var categories: List<ApiClient.Category> = emptyList()
    private var wallets: List<ApiClient.Wallet> = emptyList()
    private var goals: List<ApiClient.Goal> = emptyList()
    private var selectedMonthFilter: Calendar? = null
    private val selectedCategoryIds = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        AppSettings.applyTheme(this)
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(18))
            setBackgroundColor(color(R.color.app_background))
        }
        root.addView(header())
        root.addView(filterPanel())

        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, 0)
        }
        root.addView(ScrollView(this).apply {
            addView(list)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        })

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        loadHistory()
    }

    private fun loadHistory() {
        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            finish()
            return
        }

        ApiClient.getTransactions(token) { jsonString ->
            ApiClient.getCategories(token) { loadedCategories ->
                ApiClient.getWallets(token) { loadedWallets ->
                    ApiClient.getGoals(token) { loadedGoals ->
                        runOnUiThread {
                            categories = loadedCategories
                            wallets = loadedWallets
                            goals = loadedGoals
                            val transactions = JSONObject(jsonString).optJSONArray("transactions") ?: JSONArray()
                            allTransactions = (0 until transactions.length())
                                .map { transactions.getJSONObject(it) }
                                .sortedByDescending { it.optLong("timestamp", 0L) }
                            setupCategoryFilter()
                            renderHistory()
                        }
                    }
                }
            }
        }
    }

    private fun setupCategoryFilter() {
        selectedCategoryIds.retainAll(categories.map { it.id }.toSet())
        updateCategoryFilterLabel()
    }

    private fun renderHistory() {
        list.removeAllViews()

        val dayFilter = etDay.text.toString().trim()
        val monthFilter = selectedMonthFilter?.let { monthKey(it) } ?: ""

        val filtered = allTransactions.filter { tx ->
            val day = formatDayKey(tx.optString("timestamp"))
            val month = if (day.length >= 7) day.take(7) else ""
            val categoryId = categoryIdOf(tx)
            val categoryMatches = selectedCategoryIds.isEmpty() || (categoryId != null && categoryId in selectedCategoryIds)
            val dayMatches = dayFilter.isEmpty() || day == dayFilter
            val monthMatches = monthFilter.isEmpty() || month == monthFilter
            categoryMatches && dayMatches && monthMatches
        }

        totalText.text = buildSummary(filtered)

        if (filtered.isEmpty()) {
            list.addView(emptyState())
            return
        }

        filtered
            .groupBy { formatDayKey(it.optString("timestamp")) }
            .toSortedMap(compareByDescending { it })
            .forEach { (day, operations) ->
                list.addView(dayHeader(day))
                operations.sortedByDescending { it.optLong("timestamp", 0L) }.forEach { tx ->
                    list.addView(historyRow(tx))
                }
            }
    }

    private fun historyRow(tx: JSONObject): LinearLayout {
        val type = tx.optString("type")
        val amount = tx.optDouble("amount", 0.0)
        val title = when (type) {
            "income" -> tx.optString("description").ifBlank { categoryName(categoryIdOf(tx)) ?: "Доход" }
            "expense" -> tx.optString("description").ifBlank { categoryName(categoryIdOf(tx)) ?: "Расход" }
            "goal" -> "Цель: ${tx.optString("description").ifBlank { "накопление" }}"
            "transfer" -> tx.optString("description").ifBlank { "Перевод между счетами" }
            else -> "Операция"
        }
        val sign = when (type) {
            "income" -> "+"
            "expense", "goal" -> "-"
            else -> ""
        }
        val tint = when (type) {
            "income" -> color(R.color.income)
            "goal", "transfer" -> color(R.color.wallet)
            else -> color(R.color.expense)
        }
        val icon = when (type) {
            "income" -> "+"
            "expense" -> "-"
            "goal" -> "%"
            "transfer" -> "↔"
            else -> "·"
        }
        val visual = historyVisual(tx)
        return row(visual.icon, title, walletName(tx.optString("payment_method")), "$sign${formatMoney(amount)}", visual.backgroundColor, visual.amountColor) {
            confirmDeleteTransaction(tx)
        }
    }

    private fun historyVisual(tx: JSONObject): HistoryVisual {
        return when (tx.optString("type")) {
            "income" -> {
                val category = categoryOf(categoryIdOf(tx))
                HistoryVisual(
                    icon = category?.let { CategoryIcons.resFor(it, this) } ?: R.drawable.ic_category_salary,
                    backgroundColor = color(R.color.income),
                    amountColor = color(R.color.income)
                )
            }
            "expense" -> {
                val category = categoryOf(categoryIdOf(tx))
                HistoryVisual(
                    icon = category?.let { CategoryIcons.resFor(it, this) } ?: R.drawable.ic_category_default,
                    backgroundColor = color(R.color.expense),
                    amountColor = color(R.color.expense)
                )
            }
            "goal" -> {
                val goal = goals.firstOrNull { it.title == tx.optString("description") }
                HistoryVisual(
                    icon = goal?.let { CategoryIcons.resForGoal(it, this) } ?: R.drawable.ic_goal,
                    backgroundColor = color(R.color.goal_empty),
                    amountColor = color(R.color.goal_progress)
                )
            }
            "transfer" -> HistoryVisual(
                icon = walletIconByKey(transferWalletKey(tx.optString("payment_method"))),
                backgroundColor = color(R.color.wallet),
                amountColor = color(R.color.wallet)
            )
            else -> HistoryVisual(
                icon = R.drawable.ic_category_default,
                backgroundColor = color(R.color.accent_deep),
                amountColor = color(R.color.text_primary)
            )
        }
    }

    private fun confirmDeleteTransaction(tx: JSONObject) {
        val id = tx.optInt("id", -1)
        if (id <= 0) {
            Toast.makeText(this, "Не удалось определить операцию", Toast.LENGTH_SHORT).show()
            return
        }
        PrettyDialog.confirm(
            this,
            "Удалить операцию?",
            "Она исчезнет из истории, аналитики и расчетов на главном экране.",
            "Удалить"
        ) {
            val token = sessionManager.getToken() ?: return@confirm
            ApiClient.deleteTransaction(token, id) { success ->
                runOnUiThread {
                    Toast.makeText(this, if (success) "Операция удалена" else "Не удалось удалить", Toast.LENGTH_SHORT).show()
                    if (success) loadHistory()
                }
            }
        }
    }

    private fun header(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(14))
            addView(TextView(this@HistoryActivity).apply {
                text = "История"
                textSize = 30f
                setTextColor(color(R.color.text_primary))
                typeface = Typeface.DEFAULT_BOLD
            })
            addView(TextView(this@HistoryActivity).apply {
                text = "Операции, переводы и накопления"
                textSize = 14f
                setTextColor(color(R.color.text_secondary))
                setPadding(0, dp(4), 0, 0)
            })
        }
    }

    private fun filterPanel(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = roundedBackground(color(R.color.surface))

            totalText = TextView(this@HistoryActivity).apply {
                text = "Всего операций: 0"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(color(R.color.text_primary))
            }
            addView(totalText)

            val dateRow = LinearLayout(this@HistoryActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(12), 0, 0)
            }
            etDay = filterInput("День")
            DateInputs.attach(etDay)
            monthFilterButton = pickerButton("Все месяцы") { showMonthPicker() }

            dateRow.addView(etDay.apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f)
            })
            dateRow.addView(monthFilterButton.apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(46), 1f).apply { leftMargin = dp(10) }
            })
            addView(dateRow)
            addView(TextView(this@HistoryActivity).apply {
                text = "Категории"
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(color(R.color.text_secondary))
                setPadding(0, dp(12), 0, dp(4))
            })
            categoryFilterButton = pickerButton("Все категории") { showCategoryPicker() }
            addView(categoryFilterButton.apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(46)
                )
            })
            addView(LinearLayout(this@HistoryActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, dp(10), 0, 0)
                addView(actionButton("Применить", true).apply {
                    layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f)
                    setOnClickListener { renderHistory() }
                })
                addView(actionButton("Сбросить", false).apply {
                    layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply { leftMargin = dp(10) }
                    setOnClickListener {
                        etDay.setText("")
                        selectedMonthFilter = null
                        selectedCategoryIds.clear()
                        updateMonthFilterLabel()
                        updateCategoryFilterLabel()
                        renderHistory()
                    }
                })
            })
        }
    }

    private fun pickerButton(textValue: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 14f
            gravity = Gravity.CENTER_VERTICAL
            setSingleLine(true)
            setTextColor(color(R.color.text_primary))
            setPadding(dp(12), 0, dp(12), 0)
            background = roundedBackground(color(R.color.app_background))
            setOnClickListener { onClick() }
        }
    }

    private fun showMonthPicker() {
        val dialog = Dialog(this).apply {
            requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        val months = arrayOf(
            "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
        )
        val base = selectedMonthFilter ?: Calendar.getInstance()
        val yearMin = Calendar.getInstance().get(Calendar.YEAR) - 5
        val yearMax = Calendar.getInstance().get(Calendar.YEAR) + 3
        val monthPicker = NumberPicker(this).apply {
            minValue = 0
            maxValue = 11
            displayedValues = months
            value = base.get(Calendar.MONTH)
        }
        val yearPicker = NumberPicker(this).apply {
            minValue = yearMin
            maxValue = yearMax
            value = base.get(Calendar.YEAR).coerceIn(yearMin, yearMax)
        }
        val content = dialogContainer("Месяц")
        content.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            addView(monthPicker, LinearLayout.LayoutParams(0, dp(160), 1f))
            addView(yearPicker, LinearLayout.LayoutParams(0, dp(160), 1f))
        })
        content.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, 0)
            addView(actionButton("Все месяцы", false).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f)
                setOnClickListener {
                    selectedMonthFilter = null
                    updateMonthFilterLabel()
                    renderHistory()
                    dialog.dismiss()
                }
            })
            addView(actionButton("Выбрать", true).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply { leftMargin = dp(10) }
                setOnClickListener {
                    selectedMonthFilter = Calendar.getInstance().apply {
                        set(Calendar.YEAR, yearPicker.value)
                        set(Calendar.MONTH, monthPicker.value)
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    updateMonthFilterLabel()
                    renderHistory()
                    dialog.dismiss()
                }
            })
        })
        dialog.setContentView(content)
        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.9f).toInt(), android.view.WindowManager.LayoutParams.WRAP_CONTENT)
    }

    private fun showCategoryPicker() {
        val dialog = Dialog(this).apply {
            requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        val draft = selectedCategoryIds.toMutableSet()
        val content = dialogContainer("Категории")
        val checks = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        categories.sortedBy { it.name }.forEach { category ->
            checks.addView(CheckBox(this).apply {
                text = category.name
                textSize = 15f
                setTextColor(color(R.color.text_primary))
                buttonTintList = android.content.res.ColorStateList.valueOf(color(R.color.accent_deep))
                isChecked = category.id in draft
                setOnCheckedChangeListener { _, checked ->
                    if (checked) draft.add(category.id) else draft.remove(category.id)
                }
            })
        }
        content.addView(ScrollView(this).apply {
            addView(checks)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(260)
            )
        })
        content.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, 0)
            addView(actionButton("Сбросить", false).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f)
                setOnClickListener {
                    selectedCategoryIds.clear()
                    updateCategoryFilterLabel()
                    renderHistory()
                    dialog.dismiss()
                }
            })
            addView(actionButton("Применить", true).apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply { leftMargin = dp(10) }
                setOnClickListener {
                    selectedCategoryIds.clear()
                    selectedCategoryIds.addAll(draft)
                    updateCategoryFilterLabel()
                    renderHistory()
                    dialog.dismiss()
                }
            })
        })
        dialog.setContentView(content)
        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.9f).toInt(), android.view.WindowManager.LayoutParams.WRAP_CONTENT)
    }

    private fun dialogContainer(title: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = roundedBackground(color(R.color.surface))
            addView(TextView(this@HistoryActivity).apply {
                text = title
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(color(R.color.text_primary))
                setPadding(0, 0, 0, dp(12))
            })
        }
    }

    private fun updateMonthFilterLabel() {
        monthFilterButton.text = selectedMonthFilter?.let {
            SimpleDateFormat("LLLL yyyy", Locale("ru", "RU")).format(it.time)
                .replaceFirstChar { char -> char.titlecase(Locale("ru", "RU")) }
        } ?: "Все месяцы"
    }

    private fun updateCategoryFilterLabel() {
        categoryFilterButton.text = when (selectedCategoryIds.size) {
            0 -> "Все категории"
            1 -> categories.firstOrNull { it.id in selectedCategoryIds }?.name ?: "1 категория"
            else -> "Выбрано категорий: ${selectedCategoryIds.size}"
        }
    }

    private fun filterInput(hintText: String): EditText {
        return EditText(this).apply {
            hint = hintText
            textSize = 14f
            setSingleLine(true)
            setTextColor(color(R.color.text_primary))
            setHintTextColor(color(R.color.text_secondary))
            setPadding(dp(12), 0, dp(12), 0)
            background = roundedBackground(color(R.color.app_background))
        }
    }

    private fun actionButton(title: String, primary: Boolean): Button {
        return Button(this).apply {
            text = title
            isAllCaps = false
            textSize = 14f
            backgroundTintList = null
            setTextColor(if (primary) color(R.color.white) else color(R.color.text_primary))
            background = roundedBackground(if (primary) color(R.color.accent_deep) else color(R.color.accent_soft))
        }
    }

    private fun dayHeader(day: String): TextView {
        return TextView(this).apply {
            text = day.ifBlank { "Без даты" }
            textSize = 16f
            setTextColor(color(R.color.text_primary))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(12), 0, dp(6))
        }
    }

    private fun row(icon: Int, title: String, subtitle: String, amount: String, iconColor: Int, amountColor: Int, onClick: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = roundedBackground(color(R.color.surface))
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }

            addView(ImageView(this@HistoryActivity).apply {
                setImageResource(icon)
                setColorFilter(color(R.color.white))
                background = roundedBackground(iconColor)
                setPadding(dp(10), dp(10), dp(10), dp(10))
                layoutParams = LinearLayout.LayoutParams(dp(44), dp(44))
            })
            addView(LinearLayout(this@HistoryActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, dp(8), 0)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(this@HistoryActivity).apply {
                    text = title
                    textSize = 15f
                    setTextColor(color(R.color.text_primary))
                    typeface = Typeface.DEFAULT_BOLD
                })
                addView(TextView(this@HistoryActivity).apply {
                    text = subtitle
                    textSize = 12f
                    setTextColor(color(R.color.text_secondary))
                    setPadding(0, dp(2), 0, 0)
                })
            })
            addView(TextView(this@HistoryActivity).apply {
                text = amount
                textSize = 15f
                setTextColor(amountColor)
                typeface = Typeface.DEFAULT_BOLD
            })
        }
    }

    private fun emptyState(): TextView {
        return TextView(this).apply {
            text = "Операций за выбранный период нет"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(color(R.color.text_secondary))
            setPadding(dp(14), dp(24), dp(14), dp(24))
            background = roundedBackground(color(R.color.surface))
        }
    }

    private fun buildSummary(items: List<JSONObject>): String {
        val income = items.filter { it.optString("type") == "income" }.sumOf { it.optDouble("amount", 0.0) }
        val expense = items.filter { it.optString("type") == "expense" }.sumOf { it.optDouble("amount", 0.0) }
        return "Доходы ${formatMoney(income)} · Расходы ${formatMoney(expense)}"
    }

    private fun categoryName(id: Int?): String? = categories.firstOrNull { it.id == id }?.name

    private fun categoryOf(id: Int?): ApiClient.Category? = categories.firstOrNull { it.id == id }

    private fun categoryIdOf(tx: JSONObject): Int? {
        return when {
            tx.has("category_id") && !tx.isNull("category_id") -> tx.optInt("category_id")
            tx.has("categoryId") && !tx.isNull("categoryId") -> tx.optInt("categoryId")
            else -> null
        }
    }

    private fun walletName(value: String): String {
        val walletTitle = wallets.firstOrNull { it.key == value }?.title
        if (walletTitle != null) return walletTitle
        return when (value) {
            "cash" -> "Наличные"
            "card" -> "Безналичные"
            "goal" -> "Накопление"
            "to_cash" -> "${walletTitle("card")} -> ${walletTitle("cash")}"
            "to_card" -> "${walletTitle("cash")} -> ${walletTitle("card")}"
            else -> value
        }
    }

    private fun transferWalletKey(value: String): String {
        return when (value) {
            "to_cash" -> "cash"
            "to_card" -> "card"
            else -> value
        }
    }

    private fun walletIconByKey(key: String): Int {
        val wallet = wallets.firstOrNull { it.key == key }
        if (wallet != null) return walletIcon(wallet)
        return when (key) {
            "cash" -> R.drawable.ic_wallet_cash
            "card" -> R.drawable.ic_wallet_card
            else -> R.drawable.ic_wallet_custom
        }
    }

    private fun walletIcon(wallet: ApiClient.Wallet): Int {
        return when {
            wallet.key == "cash" && walletUsesDefaultIcon(wallet, "savings") -> R.drawable.ic_wallet_cash
            wallet.key == "card" && walletUsesDefaultIcon(wallet, "bank") -> R.drawable.ic_wallet_card
            wallet.iconKey.isNotBlank() && wallet.iconKey != "default" -> CategoryIcons.resForKey(wallet.iconKey)
            wallet.key == "cash" -> R.drawable.ic_wallet_cash
            wallet.key == "card" -> R.drawable.ic_wallet_card
            else -> R.drawable.ic_wallet_custom
        }
    }

    private fun walletUsesDefaultIcon(wallet: ApiClient.Wallet, legacyKey: String): Boolean {
        return wallet.iconKey.isBlank() || wallet.iconKey == "default" || wallet.iconKey == legacyKey
    }

    private fun walletTitle(key: String): String {
        return wallets.firstOrNull { it.key == key }?.title ?: when (key) {
            "cash" -> "Наличные"
            "card" -> "Безналичные"
            else -> key
        }
    }

    private fun formatMoney(value: Double): String = AppSettings.formatMoney(this, value)

    private fun formatDayKey(timestamp: String): String {
        val millis = timestamp.toLongOrNull() ?: return ""
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))
    }

    private fun monthKey(calendar: Calendar): String {
        return SimpleDateFormat("yyyy-MM", Locale.US).format(calendar.time)
    }

    private fun roundedBackground(fill: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(8).toFloat()
            setColor(fill)
        }
    }

    private fun color(id: Int): Int = ContextCompat.getColor(this, id)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private data class HistoryVisual(
        val icon: Int,
        val backgroundColor: Int,
        val amountColor: Int
    )
}
