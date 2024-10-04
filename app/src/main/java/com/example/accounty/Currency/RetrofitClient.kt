package com.example.accounty.Currency

import android.content.Context
import androidx.room.Room
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/"
    private var database: CurrencyDatabase? = null

    fun initialize(context: Context) {
        if (database == null) {
            database = Room.databaseBuilder(
                context.applicationContext,
                CurrencyDatabase::class.java,
                "currency_db"
            ).fallbackToDestructiveMigration()
                .build()
        }
    }

    private val gson = GsonBuilder()
        .registerTypeAdapter(CurrencyApiResponse::class.java, CurrencyApiService.CurrencyDeserializer())
        .create()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val instance: CurrencyApiService by lazy {
        retrofit.create(CurrencyApiService::class.java)
    }

    fun getDatabase(): CurrencyDatabase {
        return database ?: throw IllegalStateException("CurrencyDatabase is not initialized. Call initialize(context) first.")
    }
}