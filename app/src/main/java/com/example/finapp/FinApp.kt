package com.example.finapp

import android.app.Application

class FinApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AuthSession.install(this)
    }
}
