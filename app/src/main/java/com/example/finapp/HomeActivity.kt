package com.example.finapp

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class HomeActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var tvBalance: TextView
    private lateinit var tvIncomeTotal: TextView
    private lateinit var tvWalletsTotal: TextView
    private lateinit var tvExpensesTotal: TextView
    private lateinit var tvPeriodLabel: TextView
    private lateinit var layoutIncome: GridLayout
    private lateinit var layoutWallets: GridLayout
    private lateinit var layoutExpenses: GridLayout
    private var wallets: List<ApiClient.Wallet> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        AppSettings.applyTheme(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        sessionManager = SessionManager(this)
        tvBalance = findViewById(R.id.tvBalance)
        tvIncomeTotal = findViewById(R.id.tvIncomeTotal)
        tvWalletsTotal = findViewById(R.id.tvWalletsTotal)
        tvExpensesTotal = findViewById(R.id.tvExpensesTotal)
        tvPeriodLabel = findViewById(R.id.tvPeriodLabel)
        tvPeriodLabel.setOnClickListener { showDashboardPeriodDialog() }
        layoutIncome = findViewById(R.id.layoutIncome)
        layoutWallets = findViewById(R.id.layoutWallets)
        layoutExpenses = findViewById(R.id.layoutExpenses)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveTaskToBack(true)
            }
        })

        findViewById<Button>(R.id.btnHistory).apply {
            backgroundTintList = null
            setOnClickListener {
                startActivity(Intent(this@HomeActivity, HistoryActivity::class.java))
            }
        }
        findViewById<Button>(R.id.btnAnalytics).apply {
            backgroundTintList = null
            setOnClickListener {
                startActivity(Intent(this@HomeActivity, AnalyticsActivity::class.java))
            }
        }
        findViewById<Button>(R.id.btnSettings).apply {
            backgroundTintList = null
            setOnClickListener {
                startActivity(Intent(this@HomeActivity, SettingsActivity::class.java))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadDashboard()
    }

    private fun loadDashboard() {
        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        ApiClient.getWallets(token) { loadedWallets ->
            ApiClient.getTransactions(token) { jsonString ->
                ApiClient.getCategories(token) { categories ->
                    ApiClient.getGoals(token) { goals ->
                        runOnUiThread {
                            try {
                                wallets = loadedWallets.ifEmpty { defaultWallets() }
                                val transactions = JSONObject(jsonString).optJSONArray("transactions") ?: JSONArray()
                                renderDashboard(transactions, categories, goals)
                            } catch (e: Exception) {
                                Toast.makeText(this, "Не удалось загрузить данные", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun renderDashboard(
        transactions: JSONArray,
        categories: List<ApiClient.Category>,
        goals: List<ApiClient.Goal>
    ) {
        layoutIncome.removeAllViews()
        layoutWallets.removeAllViews()
        layoutExpenses.removeAllViews()

        val incomeItems = mutableListOf<JSONObject>()
        val expenseItems = mutableListOf<JSONObject>()
        val walletBalances = walletNames().associateWith { 0.0 }.toMutableMap()
        var expenseTotal = 0.0
        var goalsSavedInPeriod = 0.0
        val (periodStart, periodEnd) = AppSettings.dashboardPeriodMillis(this)
        tvPeriodLabel.text = "${AppSettings.dashboardPeriodLabel(this)} ▾"

        for (i in 0 until transactions.length()) {
            val tx = transactions.getJSONObject(i)
            val timestamp = tx.optString("timestamp").toLongOrNull() ?: 0L
            if (timestamp < periodStart || timestamp >= periodEnd) continue

            tx.put("_local_order", i)
            val amount = tx.optDouble("amount", 0.0)
            val type = tx.optString("type")
            val paymentMethod = tx.optString("payment_method")

            when (type) {
                "income" -> {
                    incomeItems.add(tx)
                    walletBalances[paymentMethod] = (walletBalances[paymentMethod] ?: 0.0) + amount
                }
                "expense" -> {
                    expenseTotal += amount
                    expenseItems.add(tx)
                    walletBalances[paymentMethod] = (walletBalances[paymentMethod] ?: 0.0) - amount
                }
                "transfer" -> {
                    when (paymentMethod) {
                        "to_cash" -> {
                            walletBalances["card"] = (walletBalances["card"] ?: 0.0) - amount
                            walletBalances["cash"] = (walletBalances["cash"] ?: 0.0) + amount
                        }
                        "to_card" -> {
                            walletBalances["cash"] = (walletBalances["cash"] ?: 0.0) - amount
                            walletBalances["card"] = (walletBalances["card"] ?: 0.0) + amount
                        }
                    }
                }
                "goal" -> {
                    goalsSavedInPeriod += amount
                }
            }
        }

        val plannedIncomeTotal = categories
            .filter { it.type == "income" }
            .sumOf { AppSettings.incomeAmount(this, it.id) }
        val walletTotal = walletBalances.values.sum()
        val availableBalance = walletTotal - goalsSavedInPeriod

        tvBalance.text = formatMoney(availableBalance)
        tvIncomeTotal.text = "+${formatMoney(plannedIncomeTotal)}"
        tvWalletsTotal.text = formatMoney(walletTotal)
        tvExpensesTotal.text = "-${formatMoney(expenseTotal + goalsSavedInPeriod)}"

        renderIncomeInfo(layoutIncome, categories, incomeItems)
        renderWallets(layoutWallets, walletBalances)
        renderExpenseTargets(layoutExpenses, categories, goals, expenseItems)
    }

    private fun renderIncomeInfo(
        container: GridLayout,
        categories: List<ApiClient.Category>,
        incomeItems: List<JSONObject>
    ) {
        val incomeCategories = categories
            .filter { it.type == "income" }
            .sortedBy { it.name.lowercase(Locale("ru", "RU")) }
        val actualTotals = totalsByCategoryId(incomeItems)

        incomeCategories.forEachIndexed { index, category ->
            val planned = AppSettings.incomeAmount(this, category.id)
            val amount = if (planned > 0) planned else (actualTotals[category.id] ?: 0.0)
            container.addView(
                createTile(
                    icon = CategoryIcons.resFor(category, this),
                    title = compactTitle(category.name),
                    amount = formatMoney(amount),
                    color = color(R.color.income),
                    amountColor = textSecondary(),
                    gridIndex = index,
                    twoRows = false,
                ) {
                    showEditIncomeAmountDialog(category)
                }
            )
        }

        container.addView(
            createTile(R.drawable.ic_add, "Добавить", "", color(R.color.accent_soft), textSecondary(), incomeCategories.size, false, iconTint = textSecondary()) {
                showCreateCategoryDialog("income")
            }
        )
    }

    private fun renderWallets(container: GridLayout, balances: Map<String, Double>) {
        var index = 0
        val entries = wallets.map { WalletEntry(it.key, it.title, walletIcon(it), color(R.color.wallet)) }

        entries.forEach { wallet ->
            container.addView(
                createTile(wallet.icon, compactTitle(wallet.title), formatMoney(balances[wallet.id] ?: 0.0), wallet.color, textSecondary(), index, false) {
                    when (wallet.id) {
                        "card" -> showWalletActionDialog(wallet.title, "card")
                        "cash" -> showWalletActionDialog(wallet.title, "cash")
                        else -> showWalletActionDialog(wallet.title, wallet.id)
                    }
                }
            )
            index++
        }

        container.addView(
            createTile(R.drawable.ic_add, "Добавить", "", color(R.color.accent_soft), textSecondary(), index, false, iconTint = textSecondary()) {
                showCreateWalletDialog()
            }
        )
    }

    private fun renderExpenseTargets(
        container: GridLayout,
        categories: List<ApiClient.Category>,
        goals: List<ApiClient.Goal>,
        expenseItems: List<JSONObject>
    ) {
        val expenseCategories = categories
            .filter { it.type == "expense" }
            .sortedBy { it.name.lowercase(Locale("ru", "RU")) }
        val totals = totalsByCategoryId(expenseItems)
        var index = 0

        expenseCategories.forEach { category ->
            container.addView(
                createTile(
                    icon = CategoryIcons.resFor(category, this),
                    title = compactTitle(category.name),
                    amount = formatMoney(totals[category.id] ?: 0.0),
                    color = color(R.color.expense),
                    amountColor = textSecondary(),
                    gridIndex = index,
                    twoRows = true
                ) {
                    showTransactionDialog(category.name, "expense", category.id, "Откуда списать деньги", null)
                }
            )
            index++
        }

        goals.sortedBy { it.title.lowercase(Locale("ru", "RU")) }.forEach { goal ->
            container.addView(
                createGoalTile(
                    icon = CategoryIcons.resForGoal(goal, this),
                    title = compactTitle(goal.title),
                    amount = formatMoney(goal.currentAmount),
                    amountColor = textSecondary(),
                    gridIndex = index,
                    progress = goalProgress(goal)
                ) {
                    showDepositGoalDialog(goal)
                }
            )
            index++
        }

        container.addView(
            createTile(R.drawable.ic_add, "Добавить", "", color(R.color.accent_soft), textSecondary(), index, true, iconTint = textSecondary()) {
                showAddExpenseTargetDialog()
            }
        )
    }

    private fun showWalletActionDialog(title: String, walletId: String) {
        val actions = mutableListOf(
            DialogAction("Доход", "Добавить доход в этот кошелек") {
                showIncomeCategoryPicker(walletId)
            }
        )
        if (walletId == "cash") {
            actions.add(DialogAction("Снятие наличных", "Перевести деньги из банка в наличные") {
                showTransferDialog("Снятие наличных", "to_cash")
            })
        } else if (walletId == "card") {
            actions.add(DialogAction("Пополнение наличными", "Перевести наличные в банк") {
                showTransferDialog("Пополнение наличными", "to_card")
            })
        } else {
            actions.add(DialogAction("Пополнить наличными", "Увеличить баланс кошелька") {
                showWalletDepositDialog(title, walletId, "наличными")
            })
            actions.add(DialogAction("Пополнить безналичными", "Увеличить баланс кошелька") {
                showWalletDepositDialog(title, walletId, "безналичными")
            })
        }
        PrettyDialog.options(this, title, actions)
    }

    private fun showIncomeCategoryPicker(walletId: String) {
        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) return

        ApiClient.getCategories(token) { categories ->
            runOnUiThread {
                val incomeCategories = categories.filter { it.type == "income" }.sortedBy { it.name }
                val actions = incomeCategories.map { category ->
                    DialogAction(category.name, defaultIncomeSubtitle(category)) {
                        val defaultAmount = AppSettings.incomeAmount(this, category.id).takeIf { it > 0 }
                        showTransactionDialog(category.name, "income", category.id, "Куда пришли деньги", walletId, defaultAmount)
                    }
                } + DialogAction("Другое", "Ввести сумму вручную без предустановки") {
                    showTransactionDialog("Другое", "income", null, "Куда пришли деньги", walletId, null)
                }

                PrettyDialog.options(
                    this,
                    "Источник дохода",
                    actions
                )
            }
        }
    }

    private fun showEditIncomeAmountDialog(category: ApiClient.Category) {
        val input = EditText(this).apply {
            hint = "Сумма дохода"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            val current = AppSettings.incomeAmount(this@HomeActivity, category.id)
            if (current > 0) setText(cleanAmount(current))
            selectAll()
        }
        PrettyDialog.form(this, "Сумма: ${category.name}", input) {
            val amount = input.text.toString().trim().replace(",", ".").toDoubleOrNull()
            if (amount == null || amount < 0) {
                Toast.makeText(this, "Укажите корректную сумму", Toast.LENGTH_SHORT).show()
                return@form false
            }
            AppSettings.setIncomeAmount(this, category.id, amount)
            loadDashboard()
            true
        }
    }

    private fun showWalletDepositDialog(title: String, walletId: String, source: String) {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), 0, dp(8), 0)
        }
        val amountInput = EditText(this).apply {
            hint = "Сумма"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val dateInput = dateInput()
        form.addView(amountInput)
        form.addView(dateInput)

        PrettyDialog.form(this, "Пополнить $title $source", form) {
            val token = sessionManager.getToken() ?: return@form false
            val amount = amountInput.text.toString().trim().replace(",", ".").toDoubleOrNull()
            val date = dateInput.text.toString().trim()
            if (amount == null || amount <= 0 || !DateInputs.isDate(date)) {
                Toast.makeText(this, "Заполните сумму и дату", Toast.LENGTH_SHORT).show()
                return@form false
            }

            ApiClient.createTransaction(
                token,
                "income",
                amount,
                walletId,
                null,
                DateInputs.toMillis(date),
                "Пополнение $source"
            ) { success ->
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, "Кошелек пополнен", Toast.LENGTH_SHORT).show()
                        loadDashboard()
                    } else {
                        Toast.makeText(this, "Не удалось пополнить кошелек", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            true
        }
    }

    private fun defaultIncomeSubtitle(category: ApiClient.Category): String {
        val amount = AppSettings.incomeAmount(this, category.id)
        return if (amount > 0) formatMoney(amount) else "Без заданной суммы"
    }

    private fun showTransactionDialog(
        title: String,
        type: String,
        categoryId: Int?,
        paymentTitle: String,
        preselectedWallet: String?,
        defaultAmount: Double? = null
    ) {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), 0, dp(8), 0)
        }
        val amountInput = EditText(this).apply {
            hint = "Сумма"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            defaultAmount?.let { setText(cleanAmount(it)) }
        }
        val dateInput = dateInput()
        val paymentGroup = walletRadioGroup(preselectedWallet)
        form.addView(amountInput)
        form.addView(label(paymentTitle))
        form.addView(paymentGroup)
        form.addView(dateInput)

        PrettyDialog.form(this, title, form) {
                val token = sessionManager.getToken() ?: return@form false
                val amount = amountInput.text.toString().trim().replace(",", ".").toDoubleOrNull()
                val date = dateInput.text.toString().trim()
                val paymentMethod = selectedPaymentMethod(paymentGroup)

                if (amount == null || amount <= 0 || paymentMethod.isEmpty() || !DateInputs.isDate(date)) {
                    Toast.makeText(this, "Заполните сумму, кошелек и дату", Toast.LENGTH_SHORT).show()
                    return@form false
                }

                ApiClient.createTransaction(token, type, amount, paymentMethod, categoryId, DateInputs.toMillis(date), title) { success ->
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this, "Операция добавлена", Toast.LENGTH_SHORT).show()
                            loadDashboard()
                        } else {
                            Toast.makeText(this, "Не удалось добавить операцию", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                true
        }
    }

    private fun showTransferDialog(title: String, paymentMethod: String) {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), 0, dp(8), 0)
        }
        val amountInput = EditText(this).apply {
            hint = "Сумма"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val dateInput = dateInput()
        form.addView(amountInput)
        form.addView(dateInput)

        PrettyDialog.form(this, title, form) {
                val token = sessionManager.getToken() ?: return@form false
                val amount = amountInput.text.toString().trim().replace(",", ".").toDoubleOrNull()
                val date = dateInput.text.toString().trim()
                if (amount == null || amount <= 0 || !DateInputs.isDate(date)) {
                    Toast.makeText(this, "Заполните сумму и дату", Toast.LENGTH_SHORT).show()
                    return@form false
                }

                ApiClient.createTransaction(token, "transfer", amount, paymentMethod, null, DateInputs.toMillis(date), title) { success ->
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this, "Перевод добавлен", Toast.LENGTH_SHORT).show()
                            loadDashboard()
                        } else {
                            Toast.makeText(this, "Не удалось добавить перевод", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                true
        }
    }

    private fun showDepositGoalDialog(goal: ApiClient.Goal) {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), 0, dp(8), 0)
        }
        val amountInput = EditText(this).apply {
            hint = "Сколько отложить"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val dateInput = dateInput()
        form.addView(amountInput)
        form.addView(dateInput)

        form.addView(TextView(this).apply {
            text = "Накоплено ${formatMoney(goal.currentAmount)} из ${formatMoney(goal.targetAmount)}"
            textSize = 13f
            setTextColor(textSecondary())
            setPadding(0, 0, 0, dp(8))
        }, 0)

        PrettyDialog.form(this, goal.title, form, "Отложить") {
                val token = sessionManager.getToken() ?: return@form false
                val amount = amountInput.text.toString().trim().replace(",", ".").toDoubleOrNull()
                val date = dateInput.text.toString().trim()
                if (amount == null || amount <= 0 || !DateInputs.isDate(date)) {
                    Toast.makeText(this, "Заполните сумму и дату", Toast.LENGTH_SHORT).show()
                    return@form false
                }

                ApiClient.depositGoal(token, goal.id, amount, DateInputs.toMillis(date)) { success ->
                    runOnUiThread {
                        if (success) {
                            Toast.makeText(this, "Цель пополнена", Toast.LENGTH_SHORT).show()
                            loadDashboard()
                        } else {
                            Toast.makeText(this, "Не удалось пополнить цель", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                true
        }
    }

    private fun showAddExpenseTargetDialog() {
        PrettyDialog.options(
            this,
            "Что добавить?",
            listOf(
                DialogAction("Категория расходов", "Например продукты, связь или машина") {
                    showCreateCategoryDialog("expense")
                },
                DialogAction("Цель", "Отдельная копилка или накопление") {
                    showCreateGoalDialog()
                }
            )
        )
    }

    private fun showDashboardPeriodDialog() {
        val current = AppSettings.dashboardPeriodKind(this)
        val options = listOf(
            PeriodOption("month", "Месяц", "Текущий расчетный месяц"),
            PeriodOption("quarter", "Квартал", "Последние 3 расчетных месяца"),
            PeriodOption("half_year", "Полугодие", "Последние 6 расчетных месяцев"),
            PeriodOption("year", "Год", "Последние 12 расчетных месяцев")
        )
        PrettyDialog.options(
            this,
            "Период главного экрана",
            options.map { option ->
                DialogAction(
                    title = if (option.key == current) "${option.title} ✓" else option.title,
                    subtitle = option.subtitle
                ) {
                    AppSettings.setDashboardPeriodKind(this, option.key)
                    loadDashboard()
                }
            }
        )
    }

    private fun showCreateCategoryDialog(type: String) {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), 0, dp(8), 0)
        }
        var selectedIconKey = if (type == "income") "salary" else "default"
        var iconPicked = false
        val nameInput = EditText(this).apply {
            hint = if (type == "income") "Название дохода" else "Название категории"
        }
        val amountInput = EditText(this).apply {
            hint = "Сумма дохода"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val iconPreview = ImageView(this).apply {
            setImageResource(CategoryIcons.resForKey(selectedIconKey))
            setColorFilter(Color.WHITE)
            background = roundedBackground(color(if (type == "income") R.color.income else R.color.expense))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
        }
        val iconSelector = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(10))
            setOnClickListener {
                CategoryIcons.showPicker(this@HomeActivity, selectedIconKey) { key ->
                    selectedIconKey = key
                    iconPicked = true
                    iconPreview.setImageResource(CategoryIcons.resForKey(key))
                }
            }
            addView(iconPreview)
            addView(TextView(this@HomeActivity).apply {
                text = "Выбрать иконку"
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(textPrimary())
                setPadding(dp(12), 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(this@HomeActivity).apply {
                text = "›"
                textSize = 26f
                setTextColor(textSecondary())
                gravity = Gravity.CENTER
            })
        }
        form.addView(nameInput)
        form.addView(iconSelector)
        if (type == "income") form.addView(amountInput)

        PrettyDialog.form(this, if (type == "income") "Новый доход" else "Новая категория расходов", form) {
                val token = sessionManager.getToken() ?: return@form false
                val name = nameInput.text.toString().trim()
                val amount = amountInput.text.toString().trim().replace(",", ".").toDoubleOrNull() ?: 0.0
                if (name.isEmpty()) return@form false

                ApiClient.createCategory(token, name, type) { category ->
                    runOnUiThread {
                        if (category != null) {
                            if (type == "income") AppSettings.setIncomeAmount(this, category.id, amount)
                            AppSettings.setCategoryIconKey(
                                this,
                                category.id,
                                if (iconPicked) selectedIconKey else CategoryIcons.guessKey(name)
                            )
                            Toast.makeText(this, "Добавлено", Toast.LENGTH_SHORT).show()
                            loadDashboard()
                        } else {
                            Toast.makeText(this, "Не удалось добавить", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                true
        }
    }

    private fun showCreateWalletDialog() {
        val token = sessionManager.getToken() ?: return
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), 0, dp(8), 0)
        }
        var selectedIconKey = "default"
        val input = EditText(this).apply {
            hint = "Название кошелька"
        }
        val iconPreview = ImageView(this).apply {
            setImageResource(CategoryIcons.resForKey(selectedIconKey))
            setColorFilter(Color.WHITE)
            background = roundedBackground(color(R.color.wallet))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
        }
        val iconSelector = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(10))
            setOnClickListener {
                CategoryIcons.showPicker(this@HomeActivity, selectedIconKey) { key ->
                    selectedIconKey = key
                    iconPreview.setImageResource(CategoryIcons.resForKey(key))
                }
            }
            addView(iconPreview)
            addView(TextView(this@HomeActivity).apply {
                text = "Иконка кошелька"
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(textPrimary())
                setPadding(dp(12), 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(this@HomeActivity).apply {
                text = "›"
                textSize = 26f
                setTextColor(textSecondary())
                gravity = Gravity.CENTER
            })
        }
        form.addView(input)
        form.addView(iconSelector)
        PrettyDialog.form(this, "Новый кошелек", form) {
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    ApiClient.createWallet(token, name, selectedIconKey) { wallet ->
                        runOnUiThread {
                            Toast.makeText(this, if (wallet != null) "Кошелек добавлен" else "Не удалось добавить кошелек", Toast.LENGTH_SHORT).show()
                            loadDashboard()
                        }
                    }
                    true
                } else {
                    false
                }
        }
    }

    private fun showCreateGoalDialog() {
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), 0, dp(8), 0)
        }
        var selectedIconKey = "goal"
        val titleInput = EditText(this).apply { hint = "Название цели" }
        val amountInput = EditText(this).apply {
            hint = "Сумма цели"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val deadlineInput = dateInput()
        val iconPreview = ImageView(this).apply {
            setImageResource(CategoryIcons.resForKey(selectedIconKey))
            setColorFilter(Color.WHITE)
            background = roundedBackground(color(R.color.goal_empty))
            setPadding(dp(12), dp(12), dp(12), dp(12))
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
        }
        val iconSelector = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(10))
            setOnClickListener {
                CategoryIcons.showPicker(this@HomeActivity, selectedIconKey) { key ->
                    selectedIconKey = key
                    iconPreview.setImageResource(CategoryIcons.resForKey(key))
                }
            }
            addView(iconPreview)
            addView(TextView(this@HomeActivity).apply {
                text = "Иконка цели"
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(textPrimary())
                setPadding(dp(12), 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(TextView(this@HomeActivity).apply {
                text = "›"
                textSize = 26f
                setTextColor(textSecondary())
                gravity = Gravity.CENTER
            })
        }
        form.addView(titleInput)
        form.addView(iconSelector)
        form.addView(amountInput)
        form.addView(deadlineInput)

        PrettyDialog.form(this, "Новая цель", form) {
                val token = sessionManager.getToken() ?: return@form false
                val title = titleInput.text.toString().trim()
                val amount = amountInput.text.toString().trim().replace(",", ".").toDoubleOrNull()
                val deadline = deadlineInput.text.toString().trim()
                if (title.isEmpty() || amount == null || amount <= 0 || !DateInputs.isDate(deadline)) {
                    Toast.makeText(this, "Заполните название, сумму и дату", Toast.LENGTH_SHORT).show()
                    return@form false
                }

                ApiClient.createGoal(token, title, amount, deadline) { goal ->
                    runOnUiThread {
                        if (goal != null) {
                            AppSettings.setGoalIconKey(this, goal.id, selectedIconKey)
                            Toast.makeText(this, "Цель добавлена", Toast.LENGTH_SHORT).show()
                            loadDashboard()
                        } else {
                            Toast.makeText(this, "Не удалось добавить цель", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                true
        }
    }

    private fun createTile(
        icon: Int,
        title: String,
        amount: String,
        color: Int,
        amountColor: Int,
        gridIndex: Int,
        twoRows: Boolean,
        iconTint: Int = Color.WHITE,
        onClick: (() -> Unit)?
    ): LinearLayout {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(2), dp(4), 0)
            onClick?.let { setOnClickListener { it() } }
        }
        tile.addView(ImageView(this).apply {
            setImageResource(icon)
            setColorFilter(iconTint)
            background = roundedBackground(color)
            setPadding(dp(15), dp(15), dp(15), dp(15))
            layoutParams = LinearLayout.LayoutParams(dp(62), dp(62))
        })
        tile.addView(TextView(this).apply {
            text = title
            textSize = 12f
            maxLines = 1
            gravity = Gravity.CENTER
            setTextColor(textPrimary())
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(4), 0, 0)
        })
        tile.addView(TextView(this).apply {
            text = amount
            textSize = 12f
            maxLines = 1
            gravity = Gravity.CENTER
            setTextColor(amountColor)
        })
        tile.layoutParams = GridLayout.LayoutParams().apply {
            width = dp(104)
            height = GridLayout.LayoutParams.WRAP_CONTENT
            if (twoRows) {
                rowSpec = GridLayout.spec(gridIndex % 2)
                columnSpec = GridLayout.spec(gridIndex / 2)
            } else {
                rowSpec = GridLayout.spec(0)
                columnSpec = GridLayout.spec(gridIndex)
            }
            setMargins(0, 0, dp(8), dp(6))
        }
        return tile
    }

    private fun createGoalTile(
        icon: Int,
        title: String,
        amount: String,
        amountColor: Int,
        gridIndex: Int,
        progress: Float,
        onClick: (() -> Unit)?
    ): LinearLayout {
        val tile = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(2), dp(4), 0)
            onClick?.let { setOnClickListener { it() } }
        }

        tile.addView(FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(dp(62), dp(62))
            addView(GoalProgressBackgroundView(this@HomeActivity).apply {
                emptyColor = color(R.color.goal_empty)
                progressColor = color(R.color.goal_progress)
                this.progress = progress
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            })
            addView(ImageView(this@HomeActivity).apply {
                setImageResource(icon)
                setColorFilter(Color.WHITE)
                setPadding(dp(16), dp(16), dp(16), dp(16))
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            })
        })

        tile.addView(TextView(this).apply {
            text = title
            textSize = 12f
            maxLines = 1
            gravity = Gravity.CENTER
            setTextColor(textPrimary())
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(4), 0, 0)
        })
        tile.addView(TextView(this).apply {
            text = amount
            textSize = 12f
            maxLines = 1
            gravity = Gravity.CENTER
            setTextColor(amountColor)
        })
        tile.layoutParams = GridLayout.LayoutParams().apply {
            width = dp(104)
            height = GridLayout.LayoutParams.WRAP_CONTENT
            rowSpec = GridLayout.spec(gridIndex % 2)
            columnSpec = GridLayout.spec(gridIndex / 2)
            setMargins(0, 0, dp(8), dp(6))
        }
        return tile
    }

    private fun goalProgress(goal: ApiClient.Goal): Float {
        if (goal.targetAmount <= 0.0) return 0f
        return (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
    }

    private fun totalsByCategoryId(items: List<JSONObject>): Map<Int, Double> {
        return items
            .mapNotNull { tx -> categoryIdOf(tx)?.let { id -> id to tx.optDouble("amount", 0.0) } }
            .groupBy({ it.first }, { it.second })
            .mapValues { it.value.sum() }
    }

    private fun categoryIdOf(tx: JSONObject): Int? {
        return when {
            tx.has("category_id") && !tx.isNull("category_id") -> tx.optInt("category_id")
            tx.has("categoryId") && !tx.isNull("categoryId") -> tx.optInt("categoryId")
            else -> null
        }
    }

    private fun walletRadioGroup(preselectedWallet: String?): RadioGroup {
        return RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
            walletNames().forEach { wallet ->
                addView(RadioButton(this@HomeActivity).apply {
                    id = android.view.View.generateViewId()
                    tag = wallet
                    text = walletTitle(wallet)
                    if (wallet == preselectedWallet) isChecked = true
                })
            }
        }
    }

    private fun selectedPaymentMethod(group: RadioGroup): String {
        return group.findViewById<RadioButton>(group.checkedRadioButtonId)?.tag?.toString() ?: ""
    }

    private fun walletNames(): List<String> = wallets.ifEmpty { defaultWallets() }.map { it.key }

    private fun walletTitle(id: String): String {
        return wallets.firstOrNull { it.key == id }?.title ?: when (id) {
            "cash" -> "Наличные"
            "card" -> "Безналичные"
            else -> id
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

    private fun defaultWallets(): List<ApiClient.Wallet> {
        return listOf(
            ApiClient.Wallet(0, "card", "Безналичные", "default", true),
            ApiClient.Wallet(0, "cash", "Наличные", "default", true)
        )
    }

    private fun dateInput(): EditText {
        return EditText(this).apply {
            hint = "Дата"
            setText(DateInputs.today())
            DateInputs.attach(this)
        }
    }

    private fun label(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textPrimary())
            setPadding(0, dp(10), 0, 0)
        }
    }

    private fun compactTitle(title: String): String = if (title.length <= 10) title else title.take(9) + "."

    private fun cleanAmount(value: Double): String {
        return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    }

    private fun formatMoney(value: Double): String = AppSettings.formatMoney(this, value)

    private fun roundedBackground(color: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(8).toFloat()
            setColor(color)
        }
    }

    private fun textPrimary(): Int = ContextCompat.getColor(this, R.color.text_primary)
    private fun textSecondary(): Int = ContextCompat.getColor(this, R.color.text_secondary)
    private fun color(id: Int): Int = ContextCompat.getColor(this, id)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private data class WalletEntry(val id: String, val title: String, val icon: Int, val color: Int)
    private data class PeriodOption(val key: String, val title: String, val subtitle: String)
}

private class GoalProgressBackgroundView(context: android.content.Context) : View(context) {
    var progress: Float = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }
    var emptyColor: Int = Color.TRANSPARENT
        set(value) {
            field = value
            invalidate()
        }
    var progressColor: Int = Color.TRANSPARENT
        set(value) {
            field = value
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPath = Path()
    private val clipPath = Path()
    private val sweepBounds = RectF()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = dp(8)

        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.BUTT
        paint.color = emptyColor
        canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), radius, radius, paint)

        if (progress <= 0f || width <= 0 || height <= 0) return

        val centerX = width / 2f
        val centerY = height / 2f
        val sweepRadius = kotlin.math.hypot(width.toDouble(), height.toDouble()).toFloat()
        sweepBounds.set(
            centerX - sweepRadius,
            centerY - sweepRadius,
            centerX + sweepRadius,
            centerY + sweepRadius
        )

        fillPath.reset()
        fillPath.moveTo(centerX, centerY)
        fillPath.arcTo(sweepBounds, -90f, 360f * progress)
        fillPath.close()

        clipPath.reset()
        clipPath.addRoundRect(0f, 0f, width.toFloat(), height.toFloat(), radius, radius, Path.Direction.CW)

        canvas.save()
        canvas.clipPath(clipPath)
        paint.style = Paint.Style.FILL
        paint.color = progressColor
        canvas.drawPath(fillPath, paint)
        canvas.restore()
    }

    private fun dp(value: Int): Float = value * resources.displayMetrics.density
}
