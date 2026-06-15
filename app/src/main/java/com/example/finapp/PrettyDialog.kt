package com.example.finapp

import android.app.Dialog
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

data class DialogAction(
    val title: String,
    val subtitle: String = "",
    val onClick: () -> Unit
)

object PrettyDialog {

    fun options(context: Context, title: String, actions: List<DialogAction>) {
        val dialog = base(context)
        val content = container(context)
        content.addView(titleView(context, title))
        actions.forEach { action ->
            content.addView(optionRow(context, action) {
                dialog.dismiss()
                action.onClick()
            })
        }
        content.addView(secondaryButton(context, "Отмена") { dialog.dismiss() })
        dialog.setContentView(content)
        show(dialog)
    }

    fun form(
        context: Context,
        title: String,
        body: View,
        primaryText: String = "Сохранить",
        onPrimary: () -> Boolean
    ) {
        val dialog = base(context)
        val content = container(context)
        content.addView(titleView(context, title))
        content.addView(body)
        content.addView(buttonRow(context,
            secondaryButton(context, "Отмена") { dialog.dismiss() },
            primaryButton(context, primaryText) {
                if (onPrimary()) dialog.dismiss()
            }
        ))
        dialog.setContentView(content)
        show(dialog)
    }

    fun confirm(
        context: Context,
        title: String,
        message: String,
        primaryText: String,
        onPrimary: () -> Unit
    ) {
        val dialog = base(context)
        val content = container(context)
        content.addView(titleView(context, title))
        content.addView(TextView(context).apply {
            text = message
            textSize = 14f
            setTextColor(color(context, R.color.text_secondary))
            setPadding(0, 0, 0, dp(context, 14))
        })
        content.addView(buttonRow(context,
            secondaryButton(context, "Отмена") { dialog.dismiss() },
            primaryButton(context, primaryText) {
                dialog.dismiss()
                onPrimary()
            }
        ))
        dialog.setContentView(content)
        show(dialog)
    }

    private fun base(context: Context): Dialog {
        return Dialog(context).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        }
    }

    private fun show(dialog: Dialog) {
        dialog.show()
        dialog.window?.setLayout(
            (dialog.context.resources.displayMetrics.widthPixels * 0.9f).toInt(),
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    private fun container(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 18), dp(context, 18), dp(context, 18), dp(context, 18))
            background = rounded(context, color(context, R.color.surface))
        }
    }

    private fun titleView(context: Context, title: String): TextView {
        return TextView(context).apply {
            text = title
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color(context, R.color.text_primary))
            setPadding(0, 0, 0, dp(context, 12))
        }
    }

    private fun optionRow(context: Context, action: DialogAction, onClick: () -> Unit): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12))
            background = rounded(context, color(context, R.color.app_background))
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(context, 8)
            }
            addView(TextView(context).apply {
                text = action.title
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(color(context, R.color.text_primary))
            })
            if (action.subtitle.isNotBlank()) {
                addView(TextView(context).apply {
                    text = action.subtitle
                    textSize = 13f
                    setTextColor(color(context, R.color.text_secondary))
                    setPadding(0, dp(context, 3), 0, 0)
                })
            }
        }
    }

    private fun buttonRow(context: Context, left: Button, right: Button): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(context, 14), 0, 0)
            addView(left.apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(context, 44), 1f)
            })
            addView(right.apply {
                layoutParams = LinearLayout.LayoutParams(0, dp(context, 44), 1f).apply {
                    leftMargin = dp(context, 10)
                }
            })
        }
    }

    private fun primaryButton(context: Context, title: String, onClick: () -> Unit): Button {
        return Button(context).apply {
            text = title
            isAllCaps = false
            setTextColor(color(context, R.color.white))
            background = rounded(context, color(context, R.color.accent_deep))
            setOnClickListener { onClick() }
        }
    }

    private fun secondaryButton(context: Context, title: String, onClick: () -> Unit): Button {
        return Button(context).apply {
            text = title
            isAllCaps = false
            gravity = Gravity.CENTER
            setTextColor(color(context, R.color.text_primary))
            background = rounded(context, color(context, R.color.accent_soft))
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(context, 44)
            )
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
