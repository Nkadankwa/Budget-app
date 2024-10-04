package com.example.accounty

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.accounty.budget.BudgetsActivity
import com.example.accounty.budget.DatabaseHelper
import com.example.accounty.budget.ManageActivity
import com.example.accounty.Currency.CurrencyViewModel
import com.example.accounty.Currency.RetrofitClient
import com.example.accounty.budget.TransactionsActivity
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var viewModel: CurrencyViewModel
    private lateinit var btnConvert: Button
    private lateinit var textViewResult: TextView
    private lateinit var spinnerFrom: Spinner
    private lateinit var spinnerTo: Spinner
    private lateinit var editTextFromAmount: EditText

    private lateinit var buttonAddExpense: MaterialButton
    private lateinit var buttonViewBudgets: MaterialButton
    private lateinit var buttonManage: MaterialButton

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)

        RetrofitClient.initialize(applicationContext)

        btnConvert = findViewById(R.id.btnConvert)
        textViewResult = findViewById(R.id.textViewResult)
        spinnerFrom = findViewById(R.id.spinnerFrom)
        spinnerTo = findViewById(R.id.spinnerTo)
        editTextFromAmount = findViewById(R.id.editTextFromAmount)

        buttonAddExpense = findViewById(R.id.buttonAddExpense)
        buttonViewBudgets = findViewById(R.id.buttonViewBudgets)
        buttonManage = findViewById(R.id.buttonManage)

        setupCurrencyConverter()
        setupBudgetManager()

        btnConvert.setOnClickListener{ onConvertClick(it) }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupCurrencyConverter() {
        viewModel = ViewModelProvider(this)[CurrencyViewModel::class.java]
        viewModel.deleteOldRates()
        fetchAvailableCurrencies()

        viewModel.result.observe(this) { result ->
            textViewResult.text = result
        }

        viewModel.error.observe(this) { error ->
            Toast.makeText(this, error, Toast.LENGTH_LONG).show()
        }
    }

    private fun setupBudgetManager() {
        buttonAddExpense.setOnClickListener {
            startActivity(Intent(this, TransactionsActivity::class.java))
        }

        buttonViewBudgets.setOnClickListener {
            startActivity(Intent(this, BudgetsActivity::class.java))
        }

        buttonManage.setOnClickListener {
            startActivity(Intent(this, ManageActivity::class.java))
        }
    }

    fun onConvertClick(view: View) {
        val amountText = editTextFromAmount.text.toString()
        if (amountText.isNotEmpty()) {
            val amount = amountText.toDouble()
            val fromCurrency = spinnerFrom.selectedItem.toString().lowercase()
            val toCurrency = spinnerTo.selectedItem.toString().lowercase()
            viewModel.convertCurrency(amount, fromCurrency, toCurrency)
        } else {
            Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchAvailableCurrencies() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.instance.getExchangeRates("usd")
                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("CurrencyAPI", "API Response: $body")

                    val currencies = body?.rates?.keys?.toList() ?: emptyList()

                    if (currencies.isNotEmpty()) {
                        val sortedCurrencies = currencies.map { it.uppercase() }.sorted()

                        withContext(Dispatchers.Main) {
                            val adapter = ArrayAdapter(
                                this@MainActivity,
                                android.R.layout.simple_spinner_item,
                                sortedCurrencies
                            )
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            spinnerFrom.adapter = adapter
                            spinnerTo.adapter = adapter

                            val usdIndex = sortedCurrencies.indexOf("USD")
                            if (usdIndex != -1) {
                                spinnerFrom.setSelection(usdIndex)
                            }

                            val eurIndex = sortedCurrencies.indexOf("EUR")
                            if (eurIndex != -1) {
                                spinnerTo.setSelection(eurIndex)
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@MainActivity,
                                "No currencies available.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@MainActivity,
                            "Failed to load currencies: API Error ${response.code()}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity,
                        "Failed to load currencies: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.e("CurrencyAPI", "Error fetching currencies", e)
                }
            }
        }
    }

    private fun getTotalTransactions(): Double {
        var total = 0.0
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT SUM(amount) FROM ${DatabaseHelper.TABLE_TRANSACTIONS}", null)

        cursor?.use {
            if (it.moveToFirst()) {
                total = it.getDouble(0)
            }
        }
        return total
    }

    private fun getTotalBudgetLimits(): Double {
        var total = 0.0
        val dbHelper = DatabaseHelper(this)
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT SUM(${DatabaseHelper.COLUMN_BUDGET_LIMIT}) FROM ${DatabaseHelper.TABLE_BUDGETS}", null)

        cursor?.use {
            if (it.moveToFirst()) {
                total = it.getDouble(0)
            }
        }
        return total
    }
}

