package com.example.accounty.Currency

data class CurrencyApiResponse(
    val date: String,
    val rates: Map<String, Double>
)

