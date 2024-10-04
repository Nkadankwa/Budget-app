package com.example.accounty.Currency

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CurrencyRateDao {
    @Query("SELECT * FROM currency_rates WHERE currency = :currency AND date = :date")
    suspend fun getRate(currency: String, date: String): CurrencyRate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRate(rate: CurrencyRate)

    @Query("DELETE FROM currency_rates WHERE date < :olderThan")
    suspend fun deleteOlderRates(olderThan: String)
}