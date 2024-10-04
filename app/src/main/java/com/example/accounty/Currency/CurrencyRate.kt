package com.example.accounty.Currency


import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currency_rates")
data class CurrencyRate(
    @PrimaryKey val currency: String,
    val rate: Double,
    val date: String
)
