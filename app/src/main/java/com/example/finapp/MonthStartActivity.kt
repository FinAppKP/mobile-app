package com.example.finapp

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MonthStartActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        AppSettings.applyTheme(this)
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(18))
            setBackgroundColor(color(R.color.app_background))
        }

        root.addView(header())
        root.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = rounded(color(R.color.surface))

            val preview = TextView(this@MonthStartActivity).apply {
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                setTextColor(color(R.color.text_primary))
                setPadding(0, 0, 0, dp(12))
            }
            addView(preview)

            val picker = NumberPicker(this@MonthStartActivity).apply {
                minValue = 1
                maxValue = 28
                value = AppSettings.monthStartDay(this@MonthStartActivity)
                wrapSelectorWheel = true
                setOnValueChangedListener { _, _, newValue ->
                    AppSettings.setMonthStartDay(this@MonthStartActivity, newValue)
                    preview.text = "Расчетный месяц начинается $newValue числа"
                }
            }
            addView(picker)

            preview.text = "Расчетный месяц начинается ${picker.value} числа"

            addView(Button(this@MonthStartActivity).apply {
                text = "Готово"
                isAllCaps = false
                textSize = 15f
                setTextColor(color(R.color.white))
                background = rounded(color(R.color.accent_deep))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(46)
                ).apply {
                    topMargin = dp(16)
                }
                setOnClickListener { finish() }
            })
        })

        root.addView(TextView(this).apply {
            text = "Главный экран будет показывать доходы, расходы и балансы только внутри текущего расчетного месяца. История и аналитика сохранят полный список операций."
            textSize = 14f
            setTextColor(color(R.color.text_secondary))
            setPadding(dp(4), dp(14), dp(4), 0)
        })

        setContentView(root)
    }

    private fun header(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(16))
            addView(TextView(this@MonthStartActivity).apply {
                text = "Начало месяца"
                textSize = 30f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(color(R.color.text_primary))
            })
            addView(TextView(this@MonthStartActivity).apply {
                text = "Выберите день, с которого считать период на главном экране"
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
