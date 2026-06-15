package com.example.finapp

import android.app.DatePickerDialog
import android.content.Context
import android.widget.EditText
import androidx.appcompat.app.AppCompatDelegate
import org.json.JSONArray
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object AppSettings {
    private const val PREFS = "finapp_settings"
    private const val KEY_CURRENCY_SYMBOL = "currency_symbol"
    private const val KEY_DARK = "dark_theme"
    private const val KEY_WALLETS = "custom_wallets"
    private const val KEY_MONTH_START_DAY = "month_start_day"
    private const val KEY_DASHBOARD_PERIOD = "dashboard_period"
    private const val INCOME_PREFIX = "income_amount_"
    private const val CATEGORY_ICON_PREFIX = "category_icon_"
    private const val GOAL_ICON_PREFIX = "goal_icon_"
    private const val WALLET_TITLE_PREFIX = "wallet_title_"
    private const val WALLET_ICON_PREFIX = "wallet_icon_"

    fun formatMoney(context: Context, value: Double): String {
        val symbol = currencySymbol(context)
        val formatted = NumberFormat.getNumberInstance(Locale("ru", "RU")).apply {
            maximumFractionDigits = 0
        }.format(value)
        return "$formatted $symbol"
    }

    fun currencySymbol(context: Context): String {
        return scopedPrefs(context).getString(KEY_CURRENCY_SYMBOL, "₽") ?: "₽"
    }

    fun setCurrencySymbol(context: Context, symbol: String) {
        scopedPrefs(context).edit().putString(KEY_CURRENCY_SYMBOL, symbol).apply()
    }

    fun isDarkTheme(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_DARK, false)
    }

    fun setDarkTheme(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DARK, enabled).apply()
        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun applyTheme(context: Context) {
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkTheme(context)) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun monthStartDay(context: Context): Int {
        return scopedPrefs(context).getInt(KEY_MONTH_START_DAY, 1).coerceIn(1, 28)
    }

    fun setMonthStartDay(context: Context, day: Int) {
        scopedPrefs(context).edit().putInt(KEY_MONTH_START_DAY, day.coerceIn(1, 28)).apply()
    }

    fun dashboardPeriodKind(context: Context): String {
        return scopedPrefs(context).getString(KEY_DASHBOARD_PERIOD, "month") ?: "month"
    }

    fun setDashboardPeriodKind(context: Context, kind: String) {
        scopedPrefs(context).edit().putString(KEY_DASHBOARD_PERIOD, kind).apply()
    }

    fun dashboardPeriodName(context: Context): String {
        return when (dashboardPeriodKind(context)) {
            "quarter" -> "Квартал"
            "half_year" -> "Полугодие"
            "year" -> "Год"
            else -> "Месяц"
        }
    }

    private fun dashboardPeriodMonths(context: Context): Int {
        return when (dashboardPeriodKind(context)) {
            "quarter" -> 3
            "half_year" -> 6
            "year" -> 12
            else -> 1
        }
    }

    fun dashboardPeriodMillis(context: Context, now: Calendar = Calendar.getInstance()): Pair<Long, Long> {
        val startDay = monthStartDay(context)
        val currentMonthStart = Calendar.getInstance().apply {
            timeInMillis = now.timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_MONTH, startDay)
            if (now.get(Calendar.DAY_OF_MONTH) < startDay) {
                add(Calendar.MONTH, -1)
            }
        }
        val start = currentMonthStart.clone() as Calendar
        start.add(Calendar.MONTH, -(dashboardPeriodMonths(context) - 1))
        val end = currentMonthStart.clone() as Calendar
        end.add(Calendar.MONTH, 1)
        return start.timeInMillis to end.timeInMillis
    }

    fun dashboardPeriodLabel(context: Context): String {
        val (startMillis, endMillis) = dashboardPeriodMillis(context)
        val format = SimpleDateFormat("d MMM", Locale("ru", "RU"))
        val end = Calendar.getInstance().apply {
            timeInMillis = endMillis
            add(Calendar.DAY_OF_MONTH, -1)
        }
        return "${dashboardPeriodName(context)} · ${format.format(Date(startMillis))} - ${format.format(end.time)}"
    }

    fun incomeAmount(context: Context, categoryId: Int): Double {
        return java.lang.Double.longBitsToDouble(
            scopedPrefs(context).getLong(INCOME_PREFIX + categoryId, java.lang.Double.doubleToRawLongBits(0.0))
        )
    }

    fun setIncomeAmount(context: Context, categoryId: Int, amount: Double) {
        scopedPrefs(context).edit().putLong(INCOME_PREFIX + categoryId, java.lang.Double.doubleToRawLongBits(amount)).apply()
    }

    fun categoryIconKey(context: Context, categoryId: Int): String? {
        return scopedPrefs(context).getString(CATEGORY_ICON_PREFIX + categoryId, null)
    }

    fun setCategoryIconKey(context: Context, categoryId: Int, iconKey: String) {
        scopedPrefs(context).edit().putString(CATEGORY_ICON_PREFIX + categoryId, iconKey).apply()
    }

    fun goalIconKey(context: Context, goalId: Int): String? {
        return scopedPrefs(context).getString(GOAL_ICON_PREFIX + goalId, null)
    }

    fun setGoalIconKey(context: Context, goalId: Int, iconKey: String) {
        scopedPrefs(context).edit().putString(GOAL_ICON_PREFIX + goalId, iconKey).apply()
    }

    fun customWallets(context: Context): List<String> {
        val raw = scopedPrefs(context).getString(KEY_WALLETS, "[]") ?: "[]"
        val array = JSONArray(raw)
        return (0 until array.length()).map { array.getString(it) }
    }

    fun addWallet(context: Context, name: String) {
        val wallets = customWallets(context).toMutableList()
        if (name.isNotBlank() && wallets.none { it.equals(name, ignoreCase = true) }) {
            wallets.add(name)
            saveWallets(context, wallets)
        }
    }

    fun renameWallet(context: Context, oldName: String, newName: String) {
        setWalletTitle(context, oldName, newName)
    }

    fun deleteWallet(context: Context, name: String) {
        saveWallets(context, customWallets(context).filterNot { it == name })
    }

    private fun saveWallets(context: Context, wallets: List<String>) {
        val array = JSONArray()
        wallets.forEach { array.put(it) }
        scopedPrefs(context).edit().putString(KEY_WALLETS, array.toString()).apply()
    }

    fun walletTitle(context: Context, id: String): String {
        val defaultTitle = when (id) {
            "cash" -> "Наличные"
            "card" -> "Безналичные"
            else -> id
        }
        return scopedPrefs(context).getString(WALLET_TITLE_PREFIX + id, defaultTitle) ?: defaultTitle
    }

    fun setWalletTitle(context: Context, id: String, title: String) {
        scopedPrefs(context).edit().putString(WALLET_TITLE_PREFIX + id, title).apply()
    }

    fun walletIconKey(context: Context, id: String): String? {
        return scopedPrefs(context).getString(WALLET_ICON_PREFIX + id, null)
    }

    fun setWalletIconKey(context: Context, id: String, iconKey: String) {
        scopedPrefs(context).edit().putString(WALLET_ICON_PREFIX + id, iconKey).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private fun scopedPrefs(context: Context) =
        context.getSharedPreferences("${PREFS}_${SessionManager(context).getUserKey()}", Context.MODE_PRIVATE)
}

object DateInputs {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun today(): String = dateFormat.format(Date())

    fun attach(editText: EditText) {
        editText.isFocusable = false
        editText.isClickable = true
        editText.setOnClickListener {
            val calendar = Calendar.getInstance()
            runCatching {
                val parsed = dateFormat.parse(editText.text.toString())
                if (parsed != null) calendar.time = parsed
            }
            DatePickerDialog(
                editText.context,
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    editText.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    fun toMillis(value: String): String {
        val date = dateFormat.parse(value) ?: Date()
        return date.time.toString()
    }

    fun isDate(value: String): Boolean = value.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))
}
