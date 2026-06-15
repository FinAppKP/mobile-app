package com.example.finapp

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

object AuthSession {
    private const val REDIRECT_THROTTLE_MS = 1200L
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var lastRedirectAt = 0L

    fun install(context: Context) {
        val appContext = context.applicationContext
        ApiClient.onUnauthorized = {
            redirectToLogin(appContext)
        }
    }

    fun redirectToLogin(context: Context) {
        val appContext = context.applicationContext
        val now = SystemClock.elapsedRealtime()
        if (now - lastRedirectAt < REDIRECT_THROTTLE_MS) return
        lastRedirectAt = now

        mainHandler.post {
            SessionManager(appContext).logout()
            val intent = Intent(appContext, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            appContext.startActivity(intent)
        }
    }
}
