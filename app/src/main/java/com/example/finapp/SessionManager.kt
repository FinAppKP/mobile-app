package com.example.finapp

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "auth_token"
    }

    // Сохраняем токен
    fun saveToken(token: String) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            apply()
        }
    }

    // Получаем токен
    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    // Проверяем авторизацию
    fun isLoggedIn(): Boolean {
        return getToken() != null
    }

    // Выход пользователя
    fun logout() {
        prefs.edit().clear().apply()
    }
}