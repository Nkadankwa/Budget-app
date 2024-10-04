package com.example.accounty.budget

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.accounty.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*

class TransactionsActivity : AppCompatActivity() {

    private lateinit var RecyclerViewTransactions: RecyclerView
    private lateinit var fab: FloatingActionButton
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var transactions: MutableList<Transaction>
    private lateinit var adapter: TransactionRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transactions)

        RecyclerViewTransactions = findViewById(R.id.RecyclerViewTransactions)
        fab = findViewById(R.id.fabAddTransaction)
        dbHelper = DatabaseHelper(this)
        transactions = mutableListOf()

        RecyclerViewTransactions.layoutManager = LinearLayoutManager(this)

        loadTransactions()
        adapter = TransactionRecyclerAdapter(this, transactions) { _, position ->
            deleteTransaction(position)
        }
        RecyclerViewTransactions.adapter = adapter

        fab.setOnClickListener {
            showAddTransactionDialog()
        }
    }

    private fun loadTransactions() {
        transactions.clear()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_TRANSACTIONS,
            null,
            null,
            null,
            null,
            null,
            "${DatabaseHelper.COLUMN_TRANSACTION_DATE} DESC"
        )

        cursor.use {
            if (it != null) {
                val idColumnIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)
                val amountColumnIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_AMOUNT)
                val categoryColumnIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_CATEGORY)
                val tagColumnIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_TAG)
                val dateColumnIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_DATE)
                val noteColumnIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TRANSACTION_NOTE)

                while (it.moveToNext()) {
                    val id = it.getLong(idColumnIndex)
                    val amount = it.getDouble(amountColumnIndex)
                    val categoryId = it.getLong(categoryColumnIndex)
                    val tagId = it.getLong(tagColumnIndex)
                    val date = it.getString(dateColumnIndex)
                    val note = it.getString(noteColumnIndex)

                    val categoryName = getCategoryName(categoryId)
                    val tagName = getTagName(tagId)

                    val transaction = Transaction(id, categoryName, amount, date, note)
                    transactions.add(transaction)
                }
            }
        }
        cursor.close()
    }

    private fun getCategoryName(categoryId: Long): String {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_CATEGORIES,
            arrayOf(DatabaseHelper.COLUMN_NAME),
            "${DatabaseHelper.COLUMN_ID} = ?",
            arrayOf(categoryId.toString()),
            null,
            null,
            null
        )

        cursor.use {
            if (it != null && it.moveToFirst()) {
                val categoryName = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME))
                cursor.close()
                return categoryName
            }
        }
        cursor.close()
        return "N/A"
    }

    private fun getTagName(tagId: Long): String {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_TAGS,
            arrayOf(DatabaseHelper.COLUMN_NAME),
            "${DatabaseHelper.COLUMN_ID} = ?",
            arrayOf(tagId.toString()),
            null,
            null,
            null
        )

        cursor.use {
            if (it != null && it.moveToFirst()) {
                val tagName = it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME))
                cursor.close()
                return tagName
            }
        }
        cursor.close()
        return "N/A"
    }

    private fun showAddTransactionDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_transaction, null)
        val amountEditText = dialogView.findViewById<EditText>(R.id.editTextAmount)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val tagSpinner = dialogView.findViewById<Spinner>(R.id.spinnerTag)
        val dateEditText = dialogView.findViewById<EditText>(R.id.editTextDate)
        val noteEditText = dialogView.findViewById<EditText>(R.id.editTextNote)

        val categories = getAllCategories()
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter

        val tags = getAllTags()
        val tagAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tags)
        tagAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tagSpinner.adapter = tagAdapter

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        dateEditText.setText(dateFormat.format(calendar.time))

        dateEditText.setOnClickListener {
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    dateEditText.setText(dateFormat.format(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()
        }

        AlertDialog.Builder(this)
            .setTitle("Add Transaction")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val amountText = amountEditText.text.toString().trim()

                if (TextUtils.isEmpty(amountText)) {
                    amountEditText.error = "Amount is required"
                    return@setPositiveButton
                }

                val amount = amountText.toDoubleOrNull()
                if (amount == null) {
                    amountEditText.error = "Invalid amount"
                    return@setPositiveButton
                }

                val category = categorySpinner.selectedItem.toString()
                val tag = tagSpinner.selectedItem.toString()
                val date = dateEditText.text.toString()
                val note = noteEditText.text.toString()

                addTransaction(amount, category, tag, date, note)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getAllCategories(): MutableList<String> {
        val categories = mutableListOf<String>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_CATEGORIES,
            arrayOf(DatabaseHelper.COLUMN_NAME),
            null, null, null, null, null
        )

        cursor.use {
            if (it != null) {
                val nameIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME)
                while (it.moveToNext()) {
                    categories.add(it.getString(nameIndex))
                }
            }
        }
        cursor.close()
        return categories
    }

    private fun getAllTags(): MutableList<String> {
        val tags = mutableListOf<String>()
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_TAGS,
            arrayOf(DatabaseHelper.COLUMN_NAME),
            null, null, null, null, null
        )

        cursor.use {
            if (it != null) {
                val nameIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME)
                while (it.moveToNext()) {
                    tags.add(it.getString(nameIndex))
                }
            }
        }
        cursor.close()
        return tags
    }

    private fun addTransaction(amount: Double, category: String, tag: String, date: String, note: String) {
        try {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put(DatabaseHelper.COLUMN_TRANSACTION_AMOUNT, amount)
                put(DatabaseHelper.COLUMN_TRANSACTION_CATEGORY, getCategoryId(category))
                put(DatabaseHelper.COLUMN_TRANSACTION_TAG, getTagId(tag))
                put(DatabaseHelper.COLUMN_TRANSACTION_DATE, date)
                put(DatabaseHelper.COLUMN_TRANSACTION_NOTE, note)
            }

            db.insert(DatabaseHelper.TABLE_TRANSACTIONS, null, values)
            loadTransactions()
            adapter.notifyDataSetChanged()
            db.close()
        } catch (e: Exception) {
            Log.e("TransactionsActivity", "Error adding transaction: ${e.message}")
            Toast.makeText(this, "Error adding transaction", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getCategoryId(categoryName: String): Long {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_CATEGORIES,
            arrayOf(DatabaseHelper.COLUMN_ID),
            "${DatabaseHelper.COLUMN_NAME} = ?",
            arrayOf(categoryName),
            null,
            null,
            null
        )

        cursor.use {
            if (it != null && it.moveToFirst()) {
                val categoryId = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))
                cursor.close()
                return categoryId
            }
        }
        cursor.close()
        return -1
    }

    private fun getTagId(tagName: String): Long {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            DatabaseHelper.TABLE_TAGS,
            arrayOf(DatabaseHelper.COLUMN_ID),
            "${DatabaseHelper.COLUMN_NAME} = ?",
            arrayOf(tagName),
            null,
            null,
            null
        )

        cursor.use {
            if (it != null && it.moveToFirst()) {
                val tagId = it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))
                cursor.close()
                return tagId
            }
        }
        cursor.close()
        return -1
    }

    private fun deleteTransaction(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Transaction")
            .setMessage("Are you sure you want to delete this transaction?")
            .setPositiveButton("Yes") { _, _ ->
                try {
                    val transactionToDelete = transactions[position]
                    val db = dbHelper.writableDatabase
                    val deletedRows = db.delete(
                        DatabaseHelper.TABLE_TRANSACTIONS,
                        "${DatabaseHelper.COLUMN_ID} = ?",
                        arrayOf(transactionToDelete.id.toString())
                    )
                    db.close()
                    if (deletedRows > 0) {
                        loadTransactions()
                        adapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(this, "Failed to delete transaction", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Log.e("TransactionsActivity", "Error deleting transaction: ${e.message}")
                    Toast.makeText(this, "Error deleting transaction", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}

data class Transaction(val id: Long, val category: String, val amount: Double, val date: String, val note: String)

class TransactionRecyclerAdapter(
    private val context: Context,
    private val transactions: List<Transaction>,
    private val onTransactionLongClick: (Transaction, Int) -> Unit
) :
    RecyclerView.Adapter<TransactionRecyclerAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryTextView: TextView = itemView.findViewById(R.id.textViewCategory)
        val amountTextView: TextView = itemView.findViewById(R.id.textViewAmount)
        val dateTextView: TextView = itemView.findViewById(R.id.textViewDate)

        init {
            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // We don't have direct access to the transaction here,
                    // so the long click logic will be in onBindViewHolder
                    true // Consume the long click
                } else {
                    false
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_transaction, parent, false)
        return TransactionViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val currentTransaction = transactions[position]
        holder.categoryTextView.text = "Category: ${currentTransaction.category}"
        holder.amountTextView.text = "Amount: $${currentTransaction.amount}"
        holder.dateTextView.text = "Date: ${currentTransaction.date}"

        holder.itemView.setOnLongClickListener {
            onTransactionLongClick(currentTransaction, position)
            true
        }
    }

    override fun getItemCount() = transactions.size
}