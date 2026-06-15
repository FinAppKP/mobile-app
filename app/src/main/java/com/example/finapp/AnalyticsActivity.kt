package com.example.finapp

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var chart: ExpenseChartView
    private lateinit var summary: TextView
    private lateinit var legend: LinearLayout
    private lateinit var monthTitle: TextView
    private lateinit var timeButton: Button
    private lateinit var categoryButton: Button
    private var mode = ChartMode.LINE
    private val selectedMonth = Calendar.getInstance()
    private var timeRows: List<AnalyticsRow> = emptyList()
    private var categoryRows: List<AnalyticsRow> = emptyList()

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
        root.addView(modeControls())
        root.addView(monthControls())

        summary = TextView(this).apply {
            textSize = 16f
            setLineSpacing(dp(2).toFloat(), 1f)
            setTextColor(color(R.color.text_primary))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = roundedBackground(color(R.color.surface))
        }
        root.addView(summary)

        chart = ExpenseChartView(this).apply {
            mode = this@AnalyticsActivity.mode
            background = roundedBackground(color(R.color.surface))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(300)
            ).apply {
                topMargin = dp(12)
            }
        }
        root.addView(chart)

        legend = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(12), 0, 0)
        }
        root.addView(ScrollView(this).apply {
            addView(legend)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        })

        updateModeButtons()
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        loadAnalytics()
    }

    private fun setMode(newMode: ChartMode) {
        mode = newMode
        chart.mode = newMode
        updateModeButtons()
        chart.invalidate()
        renderLegend()
    }

    private fun shiftMonth(delta: Int) {
        selectedMonth.add(Calendar.MONTH, delta)
        loadAnalytics()
    }

    private fun loadAnalytics() {
        val token = sessionManager.getToken()
        if (token.isNullOrEmpty()) {
            finish()
            return
        }

        ApiClient.getTransactions(token) { jsonString ->
            ApiClient.getCategories(token) { categories ->
                runOnUiThread {
                    val transactions = JSONObject(jsonString).optJSONArray("transactions") ?: JSONArray()
                    val categoryNames = categories.associate { it.id to it.name }
                    val expenses = (0 until transactions.length())
                        .map { transactions.getJSONObject(it) }
                        .filter { it.optString("type") == "expense" }
                        .filter { isInSelectedMonth(it.optString("timestamp")) }

                    timeRows = expenses
                        .groupBy { dayStartMillis(it.optString("timestamp")) }
                        .filterKeys { it > 0L }
                        .map { entry ->
                            AnalyticsRow(
                                title = dayLabel(entry.key),
                                value = entry.value.sumOf { it.optDouble("amount", 0.0) },
                                subtitle = "Операций: ${entry.value.size}",
                                sort = entry.key.toDouble()
                            )
                        }
                        .sortedByDescending { it.sort }

                    categoryRows = expenses
                        .groupBy { categoryNames[categoryIdOf(it)] ?: "Без категории" }
                        .map { entry ->
                            AnalyticsRow(
                                title = entry.key,
                                value = entry.value.sumOf { it.optDouble("amount", 0.0) },
                                subtitle = "Операций: ${entry.value.size}",
                                sort = entry.value.sumOf { it.optDouble("amount", 0.0) }
                            )
                        }
                        .sortedByDescending { it.value }

                    chart.byDate = timeRows.reversed().map { it.title to it.value }
                    chart.byCategory = categoryRows.map { it.title to it.value }

                    monthTitle.text = SimpleDateFormat("LLLL yyyy", Locale("ru", "RU")).format(selectedMonth.time)
                        .replaceFirstChar { it.titlecase(Locale("ru", "RU")) }
                    summary.text = buildSummary(expenses)
                    chart.invalidate()
                    renderLegend()
                }
            }
        }
    }

    private fun renderLegend() {
        legend.removeAllViews()
        val data = if (mode == ChartMode.PIE) categoryRows else timeRows
        if (data.isEmpty()) {
            legend.addView(emptyState())
            return
        }

        data.forEachIndexed { index, item ->
            val subtitle = if (mode == ChartMode.PIE && categoryRows.sumOf { it.value } > 0) {
                val total = categoryRows.sumOf { it.value }
                "${item.subtitle} · ${((item.value / total) * 100).toInt()}%"
            } else {
                item.subtitle
            }
            legend.addView(legendRow(item.title, subtitle, formatMoney(item.value), if (mode == ChartMode.PIE) chart.colorFor(index) else null))
        }
    }

    private fun header(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(14))
            addView(TextView(this@AnalyticsActivity).apply {
                text = "Аналитика"
                textSize = 30f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(color(R.color.text_primary))
            })
            addView(TextView(this@AnalyticsActivity).apply {
                text = "Расходы по дням и категориям"
                textSize = 14f
                setTextColor(color(R.color.text_secondary))
                setPadding(0, dp(4), 0, 0)
            })
        }
    }

    private fun modeControls(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
            background = roundedBackground(color(R.color.surface))
            timeButton = modeButton("По времени") { setMode(ChartMode.LINE) }
            categoryButton = modeButton("По категориям") { setMode(ChartMode.PIE) }
            addView(timeButton)
            addView(categoryButton.apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f).apply { leftMargin = dp(8) }
            })
        }
    }

    private fun monthControls(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(12), 0, dp(12))
            addView(monthButton("‹", -1))
            monthTitle = TextView(this@AnalyticsActivity).apply {
                textSize = 18f
                setTextColor(color(R.color.text_primary))
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            addView(monthTitle)
            addView(monthButton("›", 1))
        }
    }

    private fun modeButton(title: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            text = title
            isAllCaps = false
            textSize = 14f
            backgroundTintList = null
            layoutParams = LinearLayout.LayoutParams(0, dp(44), 1f)
            setOnClickListener { onClick() }
        }
    }

    private fun updateModeButtons() {
        styleModeButton(timeButton, mode == ChartMode.LINE)
        styleModeButton(categoryButton, mode == ChartMode.PIE)
    }

    private fun styleModeButton(button: Button, selected: Boolean) {
        button.setTextColor(if (selected) color(R.color.white) else color(R.color.text_primary))
        button.background = roundedBackground(if (selected) color(R.color.accent_deep) else color(R.color.accent_soft))
    }

    private fun monthButton(title: String, delta: Int): Button {
        return Button(this).apply {
            text = title
            textSize = 22f
            setTextColor(color(R.color.text_primary))
            background = roundedBackground(color(R.color.surface))
            backgroundTintList = null
            layoutParams = LinearLayout.LayoutParams(dp(54), dp(44))
            setOnClickListener { shiftMonth(delta) }
        }
    }

    private fun legendRow(title: String, subtitle: String, value: String, markerColor: Int?): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = roundedBackground(color(R.color.surface))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }

            if (markerColor != null) {
                addView(TextView(this@AnalyticsActivity).apply {
                    text = ""
                    background = roundedBackground(markerColor)
                    layoutParams = LinearLayout.LayoutParams(dp(12), dp(38)).apply { rightMargin = dp(12) }
                })
            }
            addView(LinearLayout(this@AnalyticsActivity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                addView(TextView(this@AnalyticsActivity).apply {
                    text = title
                    textSize = 16f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(color(R.color.text_primary))
                })
                addView(TextView(this@AnalyticsActivity).apply {
                    text = subtitle
                    textSize = 13f
                    setTextColor(color(R.color.text_secondary))
                    setPadding(0, dp(3), 0, 0)
                })
            })
            addView(TextView(this@AnalyticsActivity).apply {
                text = value
                textSize = 15f
                setTextColor(color(R.color.text_primary))
                typeface = Typeface.DEFAULT_BOLD
            })
        }
    }

    private fun emptyState(): TextView {
        return TextView(this).apply {
            text = "Недостаточно данных за выбранный месяц"
            textSize = 15f
            gravity = Gravity.CENTER
            setTextColor(color(R.color.text_secondary))
            setPadding(dp(14), dp(24), dp(14), dp(24))
            background = roundedBackground(color(R.color.surface))
        }
    }

    private fun buildSummary(expenses: List<JSONObject>): String {
        val total = expenses.sumOf { it.optDouble("amount", 0.0) }
        val count = expenses.size
        return "Расходы за выбранный месяц\n${formatMoney(total)} · операций: $count"
    }

    private fun categoryIdOf(tx: JSONObject): Int? {
        return when {
            tx.has("category_id") && !tx.isNull("category_id") -> tx.optInt("category_id")
            tx.has("categoryId") && !tx.isNull("categoryId") -> tx.optInt("categoryId")
            else -> null
        }
    }

    private fun dayLabel(millis: Long): String {
        return SimpleDateFormat("dd.MM", Locale("ru", "RU")).format(Date(millis))
    }

    private fun dayStartMillis(timestamp: String): Long {
        val millis = timestamp.toLongOrNull() ?: return 0L
        return Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun isInSelectedMonth(timestamp: String): Boolean {
        val millis = timestamp.toLongOrNull() ?: return false
        val calendar = Calendar.getInstance().apply { timeInMillis = millis }
        return calendar.get(Calendar.YEAR) == selectedMonth.get(Calendar.YEAR) &&
            calendar.get(Calendar.MONTH) == selectedMonth.get(Calendar.MONTH)
    }

    private fun formatMoney(value: Double): String = AppSettings.formatMoney(this, value)

    private fun roundedBackground(fill: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(8).toFloat()
            setColor(fill)
        }
    }

    private fun color(id: Int): Int = ContextCompat.getColor(this, id)
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

enum class ChartMode {
    LINE,
    PIE
}

private data class AnalyticsRow(
    val title: String,
    val value: Double,
    val subtitle: String,
    val sort: Double
)

class ExpenseChartView(context: android.content.Context) : View(context) {
    var mode = ChartMode.LINE
    var byDate: List<Pair<String, Double>> = emptyList()
    var byCategory: List<Pair<String, Double>> = emptyList()

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val fillPath = Path()
    private val colors = intArrayOf(
        Color.parseColor("#D45D5D"),
        Color.parseColor("#4C78A8"),
        Color.parseColor("#59A14F"),
        Color.parseColor("#F28E2B"),
        Color.parseColor("#B07AA1"),
        Color.parseColor("#76B7B2"),
        Color.parseColor("#E15759"),
        Color.parseColor("#EDC948"),
        Color.parseColor("#8CD17D"),
        Color.parseColor("#499894"),
        Color.parseColor("#FF9DA7"),
        Color.parseColor("#9D7660"),
        Color.parseColor("#BAB0AC"),
        Color.parseColor("#6B6ECF"),
        Color.parseColor("#B5CF6B"),
        Color.parseColor("#E7BA52"),
        Color.parseColor("#AD494A"),
        Color.parseColor("#A55194"),
        Color.parseColor("#7B4173"),
        Color.parseColor("#637939"),
        Color.parseColor("#3182BD"),
        Color.parseColor("#E6550D"),
        Color.parseColor("#31A354"),
        Color.parseColor("#756BB1")
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (mode == ChartMode.LINE) drawLineChart(canvas) else drawPieChart(canvas)
    }

    fun colorFor(index: Int): Int = colors[index % colors.size]

    private fun drawLineChart(canvas: Canvas) {
        val data = byDate
        if (data.isEmpty()) {
            drawEmpty(canvas)
            return
        }

        val left = 92f
        val top = 34f
        val right = width - 28f
        val bottom = height - 64f
        val max = niceCeiling(data.maxOf { it.second }.coerceAtLeast(1.0))

        paint.strokeWidth = 3f
        paint.textSize = 21f
        paint.typeface = Typeface.DEFAULT
        paint.textAlign = Paint.Align.RIGHT
        paint.color = color(R.color.accent_soft)
        repeat(5) { i ->
            val y = top + (bottom - top) * i / 4f
            canvas.drawLine(left, y, right, y, paint)
            paint.color = color(R.color.text_secondary)
            val value = max - max * i / 4.0
            canvas.drawText(axisLabel(value), left - 12f, y + 7f, paint)
            paint.color = color(R.color.accent_soft)
        }
        paint.textAlign = Paint.Align.LEFT

        path.reset()
        data.forEachIndexed { index, point ->
            val x = xFor(index, data.size, left, right)
            val y = yFor(point.second, max, top, bottom)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        fillPath.reset()
        fillPath.addPath(path)
        fillPath.lineTo(xFor(data.lastIndex, data.size, left, right), bottom)
        fillPath.lineTo(xFor(0, data.size, left, right), bottom)
        fillPath.close()

        fillPaint.color = withAlpha(color(R.color.expense), 36)
        canvas.drawPath(fillPath, fillPaint)

        paint.color = color(R.color.expense)
        paint.strokeWidth = 9f
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.style = Paint.Style.STROKE
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.FILL
        paint.strokeCap = Paint.Cap.BUTT

        data.forEachIndexed { index, point ->
            val x = xFor(index, data.size, left, right)
            val y = yFor(point.second, max, top, bottom)
            paint.color = color(R.color.surface)
            canvas.drawCircle(x, y, 14f, paint)
            paint.color = color(R.color.expense)
            canvas.drawCircle(x, y, 9f, paint)
        }

        paint.textSize = 22f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.CENTER
        data.forEachIndexed { index, point ->
            val x = xFor(index, data.size, left, right)
            val y = yFor(point.second, max, top, bottom)
            if (data.size <= 6 || index == 0 || index == data.lastIndex) {
                paint.color = color(R.color.text_primary)
                val label = axisLabel(point.second)
                val labelX = x.coerceIn(paint.measureText(label) / 2f + 8f, width - paint.measureText(label) / 2f - 8f)
                canvas.drawText(label, labelX, (y - 18f).coerceAtLeast(top + 18f), paint)
            }
        }

        paint.textSize = 19f
        paint.typeface = Typeface.DEFAULT_BOLD
        paint.textAlign = Paint.Align.CENTER
        val dateIndexes = visibleDateIndexes(data, left, right)
        data.forEachIndexed { index, point ->
            if (index !in dateIndexes) return@forEachIndexed

            val x = xFor(index, data.size, left, right)
            paint.color = color(R.color.accent_soft)
            paint.strokeWidth = 3f
            canvas.drawLine(x, bottom + 8f, x, bottom + 16f, paint)

            paint.color = color(R.color.text_secondary)
            val halfLabel = paint.measureText(point.first) / 2f
            val labelX = x.coerceIn(halfLabel + 8f, width - halfLabel - 8f)
            canvas.drawText(point.first, labelX, bottom + 42f, paint)
        }
        paint.textAlign = Paint.Align.LEFT
    }

    private fun drawPieChart(canvas: Canvas) {
        val data = byCategory.filter { it.second > 0 }
        if (data.isEmpty()) {
            drawEmpty(canvas)
            return
        }

        val total = data.sumOf { it.second }.coerceAtLeast(1.0)
        val size = minOf(width, height) * 0.72f
        val oval = RectF(
            width / 2f - size / 2f,
            height / 2f - size / 2f,
            width / 2f + size / 2f,
            height / 2f + size / 2f
        )

        var start = -90f
        data.forEachIndexed { index, item ->
            val sweep = (item.second / total * 360f).toFloat()
            paint.color = colorFor(index)
            canvas.drawArc(oval, start, sweep, true, paint)
            start += sweep
        }

        paint.color = color(R.color.surface)
        canvas.drawCircle(width / 2f, height / 2f, size * 0.24f, paint)
    }

    private fun drawEmpty(canvas: Canvas) {
        paint.color = color(R.color.text_secondary)
        paint.textSize = 28f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("Недостаточно данных", width / 2f, height / 2f, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    private fun xFor(index: Int, count: Int, left: Float, right: Float): Float {
        if (count <= 1) return (left + right) / 2f
        return left + (right - left) * index / (count - 1)
    }

    private fun yFor(value: Double, max: Double, top: Float, bottom: Float): Float {
        return (bottom - (bottom - top) * (value / max)).toFloat()
    }

    private fun visibleDateIndexes(data: List<Pair<String, Double>>, left: Float, right: Float): Set<Int> {
        if (data.isEmpty()) return emptySet()
        if (data.size == 1) return setOf(0)

        val maxLabelWidth = data.maxOf { paint.measureText(it.first) }
        val minGap = maxLabelWidth + 36f
        val maxLabels = (((right - left) / minGap).toInt() + 1)
            .coerceIn(2, data.size)

        if (data.size <= maxLabels) return data.indices.toSet()

        val step = data.lastIndex.toFloat() / (maxLabels - 1)
        return (0 until maxLabels)
            .mapTo(mutableSetOf()) { marker ->
                Math.round(marker * step).coerceIn(0, data.lastIndex)
            }
            .apply {
                add(0)
                add(data.lastIndex)
            }
    }

    private fun niceCeiling(value: Double): Double {
        if (value > 50000.0) {
            return kotlin.math.ceil(value / 10000.0) * 10000.0
        }
        val steps = doubleArrayOf(100.0, 500.0, 1000.0, 2000.0, 5000.0, 10000.0, 20000.0, 50000.0)
        return steps.firstOrNull { it >= value } ?: 50000.0
    }

    private fun axisLabel(value: Double): String {
        return if (value >= 10000.0) {
            "${(value / 1000).toInt()} тыс"
        } else {
            value.toInt().toString()
        }
    }

    private fun color(id: Int): Int = ContextCompat.getColor(context, id)

    private fun withAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color))
    }
}
