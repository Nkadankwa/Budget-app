package com.example.accounty.Currency

import android.app.Application

class CurrencyConverterApp : Application() {
    override fun onCreate() {
        super.onCreate()

        RetrofitClient.initialize(applicationContext)
    }
}
