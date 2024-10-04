package com.example.accounty.Currency

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class CurrencyViewModel(application: Application) : AndroidViewModel(application) {
    private val _result = MutableLiveData<String>()
    val result: LiveData<String> = _result

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private var currencyRateDao = RetrofitClient.getDatabase().currencyRateDao()
    init {
        RetrofitClient.initialize(application)
        currencyRateDao = RetrofitClient.getDatabase().currencyRateDao()
    }
    fun convertCurrency(amount: Double, fromCurrency: String, toCurrency: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.instance.getExchangeRates(fromCurrency.lowercase())
                if (response.isSuccessful) {
                    val body = response.body()
                    val rate = body?.rates?.get(toCurrency.lowercase())

                    if (rate != null) {
                        val convertedAmount = rate * amount
                        _result.value = "%.2f".format(
                            convertedAmount
                        )
                    } else {
                        _error.value = "Conversion rate not available."
                    }
                } else {
                    _error.value = "API Error: ${response.code()}"
                }
            } catch (e: Exception) {
                _error.value = "Network Error: ${e.message}"
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun deleteOldRates() {
        viewModelScope.launch {
            try {
                val oneDayAgo = LocalDate.now().minusDays(1)
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
                val dateToDelete = oneDayAgo.format(formatter)
                currencyRateDao.deleteOlderRates(dateToDelete)
            } catch (e: Exception) {
                _error.value = "Error deleting old rates: ${e.message}"
            }
        }
    }
}