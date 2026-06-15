package com.example.finapp

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.util.Locale

data class CategoryIconOption(
    val key: String,
    val title: String,
    val drawable: Int
)

object CategoryIcons {
    val options = listOf(
        CategoryIconOption("salary", "Деньги", R.drawable.ic_category_salary),
        CategoryIconOption("car", "Машина", R.drawable.ic_category_car),
        CategoryIconOption("food", "Еда", R.drawable.ic_category_food),
        CategoryIconOption("transport", "Транспорт", R.drawable.ic_category_transport),
        CategoryIconOption("home", "Дом", R.drawable.ic_category_home),
        CategoryIconOption("connection", "Связь", R.drawable.ic_category_connection),
        CategoryIconOption("health", "Здоровье", R.drawable.ic_category_health),
        CategoryIconOption("shopping", "Покупки", R.drawable.ic_category_shopping),
        CategoryIconOption("goal", "Цель", R.drawable.ic_goal),
        CategoryIconOption("savings", "Копилка", R.drawable.ic_category_savings),
        CategoryIconOption("bank", "Банк", R.drawable.ic_category_bank),
        CategoryIconOption("debt", "Долги", R.drawable.ic_category_debt),
        CategoryIconOption("subscription", "Подписки", R.drawable.ic_category_subscription),
        CategoryIconOption("gift", "Подарки", R.drawable.ic_category_gift),
        CategoryIconOption("education", "Учеба", R.drawable.ic_category_education),
        CategoryIconOption("travel", "Поездки", R.drawable.ic_category_travel),
        CategoryIconOption("sport", "Спорт", R.drawable.ic_category_sport),
        CategoryIconOption("beauty", "Красота", R.drawable.ic_category_beauty),
        CategoryIconOption("child", "Дети", R.drawable.ic_category_child),
        CategoryIconOption("fuel", "Топливо", R.drawable.ic_category_fuel),
        CategoryIconOption("repair", "Ремонт", R.drawable.ic_category_repair),
        CategoryIconOption("tax", "Налоги", R.drawable.ic_category_tax),
        CategoryIconOption("entertainment", "Досуг", R.drawable.ic_category_entertainment),
        CategoryIconOption("default", "Общее", R.drawable.ic_category_default)
    )

    fun keyFor(category: ApiClient.Category, context: Context): String {
        return AppSettings.categoryIconKey(context, category.id) ?: guessKey(category.name)
    }

    fun resFor(category: ApiClient.Category, context: Context): Int {
        return resForKey(keyFor(category, context))
    }

    fun keyForGoal(goal: ApiClient.Goal, context: Context): String {
        return AppSettings.goalIconKey(context, goal.id) ?: "goal"
    }

    fun resForGoal(goal: ApiClient.Goal, context: Context): Int {
        return resForKey(keyForGoal(goal, context))
    }

    fun resForKey(key: String): Int {
        return options.firstOrNull { it.key == key }?.drawable ?: R.drawable.ic_category_default
    }

    fun guessKey(title: String): String {
        val normalized = title.lowercase(Locale("ru", "RU"))
        return when {
            "зарплат" in normalized || "вклад" in normalized || "доход" in normalized -> "salary"
            "машин" in normalized || "авто" in normalized || "тачк" in normalized -> "car"
            "еда" in normalized || "продукт" in normalized || "кафе" in normalized -> "food"
            "транспорт" in normalized || "такси" in normalized || "передвиж" in normalized -> "transport"
            "дом" in normalized || "кварт" in normalized || "жкх" in normalized || "коммун" in normalized -> "home"
            "связ" in normalized || "интернет" in normalized -> "connection"
            "здоров" in normalized || "аптек" in normalized -> "health"
            "одеж" in normalized || "покуп" in normalized -> "shopping"
            "коп" in normalized || "накоп" in normalized || "сбереж" in normalized -> "savings"
            "банк" in normalized || "счет" in normalized || "карта" in normalized -> "bank"
            "долг" in normalized || "кредит" in normalized || "займ" in normalized -> "debt"
            "подпис" in normalized || "сервис" in normalized -> "subscription"
            "подар" in normalized -> "gift"
            "учеб" in normalized || "образ" in normalized || "курс" in normalized -> "education"
            "путеш" in normalized || "поезд" in normalized || "отпуск" in normalized -> "travel"
            "спорт" in normalized || "зал" in normalized || "фитнес" in normalized -> "sport"
            "крас" in normalized || "салон" in normalized -> "beauty"
            "дет" in normalized || "реб" in normalized -> "child"
            "бенз" in normalized || "топл" in normalized || "азс" in normalized -> "fuel"
            "ремонт" in normalized || "инстру" in normalized -> "repair"
            "налог" in normalized || "штраф" in normalized -> "tax"
            "кино" in normalized || "игр" in normalized || "досуг" in normalized || "развлеч" in normalized -> "entertainment"
            else -> "default"
        }
    }

    fun showPicker(context: Context, currentKey: String, onSelected: (String) -> Unit) {
        val dialog = Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 18), dp(context, 18), dp(context, 18), dp(context, 18))
            background = rounded(context, color(context, R.color.surface))
        }
        content.addView(TextView(context).apply {
            text = "Выберите иконку"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color(context, R.color.text_primary))
            setPadding(0, 0, 0, dp(context, 12))
        })

        val grid = GridLayout(context).apply {
            columnCount = 4
            rowCount = 6
        }
        options.forEach { option ->
            grid.addView(iconCell(context, option, option.key == currentKey) {
                dialog.dismiss()
                onSelected(option.key)
            })
        }
        content.addView(grid)
        dialog.setContentView(content)
        dialog.show()
        dialog.window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.9f).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun iconCell(context: Context, option: CategoryIconOption, selected: Boolean, onClick: () -> Unit): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(context, 6), dp(context, 8), dp(context, 6), dp(context, 8))
            setOnClickListener { onClick() }
            layoutParams = GridLayout.LayoutParams().apply {
                width = (context.resources.displayMetrics.widthPixels * 0.9f / 4f).toInt() - dp(context, 14)
                height = GridLayout.LayoutParams.WRAP_CONTENT
                setMargins(0, 0, 0, dp(context, 8))
            }
            addView(ImageView(context).apply {
                setImageResource(option.drawable)
                setColorFilter(if (selected) color(context, R.color.white) else color(context, R.color.text_primary))
                background = rounded(context, if (selected) color(context, R.color.accent_deep) else color(context, R.color.accent_soft))
                setPadding(dp(context, 13), dp(context, 13), dp(context, 13), dp(context, 13))
                layoutParams = LinearLayout.LayoutParams(dp(context, 52), dp(context, 52))
            })
            addView(TextView(context).apply {
                text = option.title
                textSize = 12f
                maxLines = 1
                gravity = Gravity.CENTER
                setTextColor(color(context, R.color.text_secondary))
                setPadding(0, dp(context, 6), 0, 0)
            })
        }
    }

    private fun rounded(context: Context, fill: Int): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dp(context, 8).toFloat()
            setColor(fill)
        }
    }

    private fun color(context: Context, id: Int): Int = ContextCompat.getColor(context, id)
    private fun dp(context: Context, value: Int): Int = (value * context.resources.displayMetrics.density).toInt()
}
