package com.example.finapp

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER = "user_key"
    }

    // Сохраняем токен
    fun saveToken(token: String, userKey: String = "") {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            if (userKey.isNotBlank()) putString(KEY_USER, userKey.lowercase())
            apply()
        }
    }

    // Получаем токен
    fun getToken(): String? {
        return prefs.getString(KEY_TOKEN, null)
    }

    fun getUserKey(): String {
        return prefs.getString(KEY_USER, null) ?: getToken().orEmpty().take(16).ifBlank { "guest" }
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
