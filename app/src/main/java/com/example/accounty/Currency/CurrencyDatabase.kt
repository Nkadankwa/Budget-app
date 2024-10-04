package com.example.accounty.Currency

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CurrencyRate::class], version = 1)
abstract class CurrencyDatabase : RoomDatabase() {
    abstract fun currencyRateDao(): CurrencyRateDao
}