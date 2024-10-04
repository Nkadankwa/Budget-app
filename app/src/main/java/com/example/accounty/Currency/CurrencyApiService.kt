package com.example.accounty.Currency

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonParseException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.lang.reflect.Type

interface CurrencyApiService {
    @GET("{currency}.json")
    suspend fun getExchangeRates(
        @Path("currency") currency: String
    ): Response<CurrencyApiResponse>

    companion object {
        fun create(): CurrencyApiService {
            val gson = GsonBuilder()
                .registerTypeAdapter(CurrencyApiResponse::class.java, CurrencyDeserializer())
                .create()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies/")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()

            return retrofit.create(CurrencyApiService::class.java)
        }
    }

    class CurrencyDeserializer : JsonDeserializer<CurrencyApiResponse> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): CurrencyApiResponse {
            val jsonObject = json?.asJsonObject ?: throw JsonParseException("Expected JSON object")

            val date = jsonObject.get("date").asString
            val rates = mutableMapOf<String, Double>()

            jsonObject.entrySet().forEach { (key, value) ->
                if (key != "date" && value.isJsonObject) {
                    value.asJsonObject.entrySet().forEach { (currency, rate) ->
                        rates[currency] = rate.asDouble
                    }
                }
            }

            return CurrencyApiResponse(date, rates)
        }
    }
}