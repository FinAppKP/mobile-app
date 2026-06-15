package com.example.finapp

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.util.Locale

class CurrencyActivity : AppCompatActivity() {

    private lateinit var list: LinearLayout

    private val currencies = listOf(
        CurrencyChoice("RUB", "₽", "Российский рубль", "Россия"),
        CurrencyChoice("USD", "$", "Доллар США", "США"),
        CurrencyChoice("EUR", "€", "Евро", "Евросоюз"),
        CurrencyChoice("GBP", "£", "Фунт стерлингов", "Великобритания"),
        CurrencyChoice("CNY", "¥", "Китайский юань", "Китай"),
        CurrencyChoice("JPY", "¥", "Японская иена", "Япония"),
        CurrencyChoice("KZT", "₸", "Казахстанский тенге", "Казахстан"),
        CurrencyChoice("TRY", "₺", "Турецкая лира", "Турция"),
        CurrencyChoice("AED", "د.إ", "Дирхам ОАЭ", "ОАЭ"),
        CurrencyChoice("BYN", "Br", "Белорусский рубль", "Беларусь")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        AppSettings.applyTheme(this)
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(18))
            setBackgroundColor(color(R.color.app_background))
        }
        root.addView(title("Валюта", "Выберите валюту, и знак обновится во всем приложении"))

        val search = EditText(this).apply {
            hint = "Поиск: валюта или страна"
            textSize = 16f
            setSingleLine(true)
            setPadding(dp(14), 0, dp(14), 0)
            background = rounded(color(R.color.surface))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(50)
            ).apply {
                bottomMargin = dp(12)
            }
        }
        root.addView(search)

        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(ScrollView(this).apply {
            addView(list)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        })

        search.addTextChangedListener(SimpleTextWatcher { render(search.text.toString()) })
        setContentView(root)
        render("")
    }

    private fun render(query: String) {
        list.removeAllViews()
        val normalized = query.lowercase(Locale("ru", "RU"))
        currencies
            .filter {
                normalized.isBlank() ||
                    it.code.lowercase(Locale.US).contains(normalized) ||
                    it.name.lowercase(Locale("ru", "RU")).contains(normalized) ||
                    it.country.lowercase(Locale("ru", "RU")).contains(normalized)
            }
            .forEach { currency ->
                list.addView(currencyRow(currency))
            }
    }

    private fun currencyRow(currency: CurrencyChoice): LinearLayout {
        val selected = AppSettings.currencySymbol(this) == currency.symbol
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(if (selected) color(R.color.accent_soft) else color(R.color.surface))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
            setOnClickListener {
                AppSettings.setCurrencySymbol(this@CurrencyActivity, currency.symbol)
                finish()
            }

            addView(TextView(this@CurrencyActivity).apply {
                text = currency.symbol
                textSize = 24f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (selected) color(R.color.text_primary) else color(R.color.wallet))
                layoutParams = LinearLayout.LayoutParams(dp(54), LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            addView(LinearLayout(this@CurrencyActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(this@CurrencyActivity).apply {
                    text = currency.name
                    textSize = 16f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(color(R.color.text_primary))
                })
                addView(TextView(this@CurrencyActivity).apply {
                    text = "${currency.country}, ${currency.code}"
                    textSize = 13f
                    setTextColor(color(R.color.text_secondary))
                })
            })
        }
    }

    private fun title(title: String, subtitle: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(16))
            addView(TextView(this@CurrencyActivity).apply {
                text = title
                textSize = 30f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(color(R.color.text_primary))
            })
            addView(TextView(this@CurrencyActivity).apply {
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

    private data class CurrencyChoice(val code: String, val symbol: String, val name: String, val country: String)
}
