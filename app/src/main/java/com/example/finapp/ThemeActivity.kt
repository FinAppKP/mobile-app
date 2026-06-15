package com.example.finapp

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class ThemeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        AppSettings.applyTheme(this)
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(18))
            setBackgroundColor(color(R.color.app_background))
        }
        root.addView(title("Цветовая схема", "Выберите оформление приложения"))
        root.addView(themeCard("Светлая", "Чистый дневной интерфейс", false))
        root.addView(themeCard("Темная", "Более мягкий экран вечером", true))

        setContentView(root)
    }

    private fun themeCard(title: String, subtitle: String, dark: Boolean): LinearLayout {
        val selected = AppSettings.isDarkTheme(this) == dark
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = rounded(if (selected) color(R.color.accent_soft) else color(R.color.surface))
            setOnClickListener {
                AppSettings.setDarkTheme(this@ThemeActivity, dark)
                recreate()
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }

            addView(TextView(this@ThemeActivity).apply {
                text = if (selected) "$title  выбрана" else title
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(color(R.color.text_primary))
            })
            addView(TextView(this@ThemeActivity).apply {
                text = subtitle
                textSize = 14f
                setTextColor(color(R.color.text_secondary))
                setPadding(0, dp(5), 0, dp(12))
            })
            addView(LinearLayout(this@ThemeActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                themePalette(dark).forEach { hex ->
                    addView(swatch(hex))
                }
            })
        }
    }

    private fun themePalette(dark: Boolean): List<String> {
        return if (dark) {
            listOf("#11161D", "#1B222C", "#6DC09A", "#8FA7CC", "#D98780")
        } else {
            listOf("#F5F6F8", "#FFFFFF", "#2E7D5B", "#45658F", "#B65A52")
        }
    }

    private fun swatch(hex: String): TextView {
        return TextView(this).apply {
            background = rounded(android.graphics.Color.parseColor(hex))
            layoutParams = LinearLayout.LayoutParams(dp(36), dp(36)).apply {
                rightMargin = dp(8)
            }
        }
    }

    private fun title(title: String, subtitle: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(16))
            addView(TextView(this@ThemeActivity).apply {
                text = title
                textSize = 30f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(color(R.color.text_primary))
            })
            addView(TextView(this@ThemeActivity).apply {
                text = subtitle
                textSize = 14f
                setTextColor(color(R.color.text_secondary))
                setPadding(0, dp(4), 0, 0)
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
