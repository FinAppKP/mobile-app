package com.example.finapp

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.ImageView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class CategoryEditorActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var content: LinearLayout
    private var wallets: List<ApiClient.Wallet> = emptyList()
    private var goals: List<ApiClient.Goal> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        AppSettings.applyTheme(this)
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), 0)
            setBackgroundColor(color(R.color.app_background))
        }
        root.addView(title("Категории", "Редактируйте доходы, кошельки, расходы и цели"))

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(18))
        }
        root.addView(ScrollView(this).apply {
            addView(content)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        })

        setContentView(root)
        loadData()
    }

    private fun loadData() {
        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            finish()
            return
        }

        ApiClient.getCategories(token) { categories ->
            ApiClient.getWallets(token) { loadedWallets ->
                ApiClient.getGoals(token) { loadedGoals ->
                    runOnUiThread {
                        wallets = loadedWallets
                        goals = loadedGoals
                        content.removeAllViews()
                        addCategorySection("Доходы", categories.filter { it.type == "income" })
                        addWalletSection()
                        addCategorySection("Расходы", categories.filter { it.type == "expense" })
                        addGoalSection()
                    }
                }
            }
        }
    }

    private fun addCategorySection(title: String, categories: List<ApiClient.Category>) {
        content.addView(sectionTitle(title))
        categories.sortedBy { it.name }.forEach { category ->
            content.addView(row(
                category.name,
                CategoryIcons.resFor(category, this),
                if (category.type == "income") R.color.income else R.color.expense
            ) {
                showCategoryActions(category)
            })
        }
    }

    private fun addWalletSection() {
        content.addView(sectionTitle("Кошельки"))
        wallets.forEach { wallet ->
            content.addView(row(wallet.title, walletIcon(wallet), R.color.wallet) {
                showWalletActions(wallet)
            })
        }
    }

    private fun addGoalSection() {
        content.addView(sectionTitle("Цели"))
        if (goals.isEmpty()) {
            content.addView(emptyRow("Целей пока нет"))
            return
        }
        goals.sortedBy { it.title }.forEach { goal ->
            content.addView(row(goal.title, CategoryIcons.resForGoal(goal, this), R.color.goal_empty) {
                showGoalActions(goal)
            })
        }
    }

    private fun showCategoryActions(category: ApiClient.Category) {
        val token = sessionManager.getToken() ?: return
        PrettyDialog.options(
            this,
            category.name,
            listOf(
                DialogAction("Переименовать", "Изменить название категории") {
                    renameCategory(token, category)
                },
                DialogAction("Поменять иконку", "Выбрать другой значок для категории") {
                    changeCategoryIcon(category)
                },
                DialogAction("Удалить", "Убрать категорию из списка") {
                    ApiClient.deleteCategory(token, category.id) { success ->
                        runOnUiThread {
                            Toast.makeText(this, if (success) "Удалено" else "Не удалось удалить", Toast.LENGTH_SHORT).show()
                            loadData()
                        }
                    }
                }
            )
        )
    }

    private fun renameCategory(token: String, category: ApiClient.Category) {
        val input = EditText(this).apply {
            setText(category.name)
            selectAll()
        }
        PrettyDialog.form(this, "Переименовать", input) {
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    ApiClient.updateCategory(token, category.id, newName, category.type) { success ->
                        runOnUiThread {
                            Toast.makeText(this, if (success) "Сохранено" else "Не удалось сохранить", Toast.LENGTH_SHORT).show()
                            loadData()
                        }
                    }
                    true
                } else {
                    false
                }
        }
    }

    private fun changeCategoryIcon(category: ApiClient.Category) {
        CategoryIcons.showPicker(this, CategoryIcons.keyFor(category, this)) { key ->
            AppSettings.setCategoryIconKey(this, category.id, key)
            loadData()
        }
    }

    private fun showWalletActions(wallet: ApiClient.Wallet) {
        val token = sessionManager.getToken() ?: return
        val actions = mutableListOf(
            DialogAction("Переименовать", "Изменить название кошелька") {
                val input = EditText(this).apply {
                    setText(wallet.title)
                    selectAll()
                }
                PrettyDialog.form(this, "Переименовать", input) {
                        val newName = input.text.toString().trim()
                        if (newName.isNotEmpty()) {
                            ApiClient.updateWallet(token, wallet.key, newName, null) { updated ->
                                runOnUiThread {
                                    Toast.makeText(this, if (updated != null) "Сохранено" else "Не удалось сохранить", Toast.LENGTH_SHORT).show()
                                    loadData()
                                }
                            }
                            true
                        } else {
                            false
                        }
                }
            },
            DialogAction("Поменять иконку", "Выбрать другой значок для кошелька") {
                changeWalletIcon(wallet)
            }
        )
        if (wallet.isSystem) {
            actions.add(DialogAction("Удалить", "Стандартный кошелек нельзя удалить") {
                Toast.makeText(this, "Стандартный кошелек нельзя удалить", Toast.LENGTH_SHORT).show()
            })
        } else {
            actions.add(DialogAction("Удалить", "Убрать кошелек из списка") {
                ApiClient.deleteWallet(token, wallet.key) { success ->
                    runOnUiThread {
                        Toast.makeText(this, if (success) "Кошелек удален" else "Не удалось удалить", Toast.LENGTH_SHORT).show()
                        loadData()
                    }
                }
            })
        }
        PrettyDialog.options(
            this,
            wallet.title,
            actions
        )
    }

    private fun changeWalletIcon(wallet: ApiClient.Wallet) {
        val token = sessionManager.getToken() ?: return
        CategoryIcons.showPicker(this, walletIconKey(wallet)) { key ->
            ApiClient.updateWallet(token, wallet.key, null, key) { updated ->
                runOnUiThread {
                    Toast.makeText(this, if (updated != null) "Иконка сохранена" else "Не удалось сохранить", Toast.LENGTH_SHORT).show()
                    loadData()
                }
            }
        }
    }

    private fun showGoalActions(goal: ApiClient.Goal) {
        val token = sessionManager.getToken() ?: return
        PrettyDialog.options(
            this,
            goal.title,
            listOf(
                DialogAction("Поменять иконку", "Выбрать другой значок для цели") {
                    changeGoalIcon(goal)
                },
                DialogAction("Удалить", "Убрать цель и связанные накопления") {
                    ApiClient.deleteGoal(token, goal.id) { success ->
                        runOnUiThread {
                            Toast.makeText(this, if (success) "Цель удалена" else "Не удалось удалить цель", Toast.LENGTH_SHORT).show()
                            loadData()
                        }
                    }
                }
            )
        )
    }

    private fun changeGoalIcon(goal: ApiClient.Goal) {
        CategoryIcons.showPicker(this, CategoryIcons.keyForGoal(goal, this)) { key ->
            AppSettings.setGoalIconKey(this, goal.id, key)
            loadData()
        }
    }

    private fun sectionTitle(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color(R.color.text_primary))
            setPadding(0, dp(16), 0, dp(8))
        }
    }

    private fun row(title: String, icon: Int, tint: Int, onClick: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(color(R.color.surface))
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }

            addView(ImageView(this@CategoryEditorActivity).apply {
                setImageResource(icon)
                setColorFilter(color(R.color.white))
                background = rounded(color(tint))
                setPadding(dp(10), dp(10), dp(10), dp(10))
                layoutParams = LinearLayout.LayoutParams(dp(42), dp(42))
            })
            addView(TextView(this@CategoryEditorActivity).apply {
                text = title
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(color(R.color.text_primary))
                setPadding(dp(12), 0, 0, 0)
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, dp(42), 1f)
            })
            addView(TextView(this@CategoryEditorActivity).apply {
                text = "›"
                textSize = 26f
                setTextColor(color(R.color.text_secondary))
                gravity = android.view.Gravity.CENTER
            })
        }
    }

    private fun emptyRow(textValue: String): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 14f
            setTextColor(color(R.color.text_secondary))
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = rounded(color(R.color.surface))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
        }
    }

    private fun title(title: String, subtitle: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(8))
            addView(TextView(this@CategoryEditorActivity).apply {
                text = title
                textSize = 30f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(color(R.color.text_primary))
            })
            addView(TextView(this@CategoryEditorActivity).apply {
                text = subtitle
                textSize = 14f
                setTextColor(color(R.color.text_secondary))
                setPadding(0, dp(4), 0, 0)
            })
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

    private fun walletIconKey(wallet: ApiClient.Wallet): String {
        if (wallet.key == "cash" && walletUsesDefaultIcon(wallet, "savings")) return "default"
        if (wallet.key == "card" && walletUsesDefaultIcon(wallet, "bank")) return "default"
        return wallet.iconKey.takeIf { it.isNotBlank() && it != "default" } ?: when (wallet.key) {
            "cash" -> "default"
            "card" -> "default"
            else -> "default"
        }
    }

    private fun walletUsesDefaultIcon(wallet: ApiClient.Wallet, legacyKey: String): Boolean {
        return wallet.iconKey.isBlank() || wallet.iconKey == "default" || wallet.iconKey == legacyKey
    }

    private fun rounded(fill: Int): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dp(8).toFloat()
            setColor(fill)
        }
    }

    private fun color(id: Int): Int = ContextCompat.getColor(this, id)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
