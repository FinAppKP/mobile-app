package com.example.finapp

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        AppSettings.applyTheme(this)
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(color(R.color.app_background))
        }

        root.addView(header("Настройки", "Период, валюта, тема, категории и аккаунт"))
        root.addView(ScrollView(this).apply {
            addView(LinearLayout(this@SettingsActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(18), dp(8), dp(18), dp(18))

                addView(settingsCard("Начало месяца", "Для расчетов на главном экране", AppSettings.monthStartDay(this@SettingsActivity).toString()) {
                    startActivity(Intent(this@SettingsActivity, MonthStartActivity::class.java))
                })
                addView(settingsCard("Валюта", "Поиск по названию валюты или страны", AppSettings.currencySymbol(this@SettingsActivity)) {
                    startActivity(Intent(this@SettingsActivity, CurrencyActivity::class.java))
                })
                addView(settingsCard("Цветовая схема", if (AppSettings.isDarkTheme(this@SettingsActivity)) "Темная тема включена" else "Сейчас светлая тема", "Aa") {
                    startActivity(Intent(this@SettingsActivity, ThemeActivity::class.java))
                })
                addView(settingsCard("Редактирование категорий", "Доходы, кошельки и расходы", "⋯") {
                    startActivity(Intent(this@SettingsActivity, CategoryEditorActivity::class.java))
                })
                addView(settingsCard("Выйти", "Завершить текущую сессию", "×") {
                    confirmLogout()
                }.apply {
                    setPadding(dp(16), dp(14), dp(16), dp(14))
                })
            })
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        })

        setContentView(root)
    }

    private fun confirmLogout() {
        PrettyDialog.confirm(
            this,
            "Выйти из аккаунта?",
            "Текущая сессия будет завершена. Данные останутся в аккаунте.",
            "Выйти"
        ) {
                sessionManager.logout()
                startActivity(Intent(this, LoginActivity::class.java))
                finishAffinity()
        }
    }

    private fun header(title: String, subtitle: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(14))
            addView(TextView(this@SettingsActivity).apply {
                text = title
                textSize = 30f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(color(R.color.text_primary))
            })
            addView(TextView(this@SettingsActivity).apply {
                text = subtitle
                textSize = 14f
                setTextColor(color(R.color.text_secondary))
                setPadding(0, dp(4), 0, 0)
            })
        }
    }

    private fun settingsCard(title: String, subtitle: String, mark: String, onClick: () -> Unit): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = rounded(color(R.color.surface))
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }

            addView(TextView(this@SettingsActivity).apply {
                text = mark
                textSize = 22f
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(color(R.color.white))
                background = rounded(color(R.color.accent_deep))
                layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
            })
            addView(LinearLayout(this@SettingsActivity).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(14), 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(this@SettingsActivity).apply {
                    text = title
                    textSize = 17f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(color(R.color.text_primary))
                })
                addView(TextView(this@SettingsActivity).apply {
                    text = subtitle
                    textSize = 13f
                    setTextColor(color(R.color.text_secondary))
                    setPadding(0, dp(3), 0, 0)
                })
            })
            addView(TextView(this@SettingsActivity).apply {
                text = "›"
                textSize = 28f
                setTextColor(color(R.color.text_secondary))
            })
        }
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
