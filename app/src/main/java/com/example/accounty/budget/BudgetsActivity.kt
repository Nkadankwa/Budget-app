package com.example.accounty.budget

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
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

class BudgetsActivity : AppCompatActivity() {

    private lateinit var budgetsRecyclerView: RecyclerView
    private lateinit var budgets: MutableList<Budget>
    private lateinit var adapter: BudgetRecyclerAdapter
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var buttonAddBudget: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_budgets)

        budgetsRecyclerView = findViewById(R.id.listViewBudgets)
        buttonAddBudget = findViewById(R.id.buttonAddBudget)
        dbHelper = DatabaseHelper(this)
        budgets = mutableListOf()

        budgetsRecyclerView.layoutManager = LinearLayoutManager(this)
        adapter = BudgetRecyclerAdapter(this, budgets) { budget, position, action ->
            when (action) {
                "edit" -> showEditBudgetDialog(position)
                "delete" -> deleteBudget(position)
            }
        }
        budgetsRecyclerView.adapter = adapter

        loadBudgets()

        buttonAddBudget.setOnClickListener {
            showAddBudgetDialog()
        }
    }

    private fun loadBudgets() {
        budgets.clear()
        val db = dbHelper.readableDatabase
        val cursor: Cursor? = db.query(
            DatabaseHelper.TABLE_BUDGETS,
            null,
            null,
            null,
            null,
            null,
            null
        )

        cursor?.use {
            val idColumnIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)
            val categoryColumnIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BUDGET_CATEGORY)
            val limitColumnIndex = it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_BUDGET_LIMIT)

            while (it.moveToNext()) {
                val id = it.getLong(idColumnIndex)
                val categoryId = it.getLong(categoryColumnIndex)
                val limit = it.getDouble(limitColumnIndex)

                val categoryName = getCategoryName(categoryId)
                val spent = dbHelper.getTotalSpentForCategory(categoryId)
                budgets.add(Budget(id, categoryName, limit, spent))
            }
        }
        adapter.notifyDataSetChanged()
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

        cursor?.use {
            if (it.moveToFirst()) {
                return it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME))
            }
        }
        return "N/A"
    }

    private fun showAddBudgetDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_budget, null)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val limitEditText = dialogView.findViewById<EditText>(R.id.editTextLimit)

        val categories = getAllCategories()
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter

        AlertDialog.Builder(this)
            .setTitle("Add Budget")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val limitText = limitEditText.text.toString().trim()
                if (TextUtils.isEmpty(limitText)) {
                    Toast.makeText(this, "Limit is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val limit = limitText.toDoubleOrNull()
                if (limit == null) {
                    Toast.makeText(this, "Invalid limit", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val category = categorySpinner.selectedItem.toString()
                addBudget(category, limit)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditBudgetDialog(position: Int) {
        val budget = budgets[position]
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_budget, null)
        val categorySpinner = dialogView.findViewById<Spinner>(R.id.spinnerCategory)
        val limitEditText = dialogView.findViewById<EditText>(R.id.editTextLimit)

        val categories = getAllCategories()
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        categorySpinner.adapter = categoryAdapter

        val categoryIndex = categories.indexOf(budget.category)
        if (categoryIndex >= 0) {
            categorySpinner.setSelection(categoryIndex)
        }
        limitEditText.setText(budget.limit.toString())

        AlertDialog.Builder(this)
            .setTitle("Edit Budget")
            .setView(dialogView)
            .setPositiveButton("Update") { _, _ ->
                val limitText = limitEditText.text.toString().trim()
                if (TextUtils.isEmpty(limitText)) {
                    Toast.makeText(this, "Limit is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val limit = limitText.toDoubleOrNull()
                if (limit == null) {
                    Toast.makeText(this, "Invalid limit", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val category = categorySpinner.selectedItem.toString()
                updateBudget(budget.id, category, limit)
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

        cursor?.use {
            while (it.moveToNext()) {
                categories.add(it.getString(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NAME)))
            }
        }
        return categories
    }

    private fun addBudget(category: String, limit: Double) {
        try {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put(DatabaseHelper.COLUMN_BUDGET_CATEGORY, getCategoryId(category))
                put(DatabaseHelper.COLUMN_BUDGET_LIMIT, limit)
            }

            db.insert(DatabaseHelper.TABLE_BUDGETS, null, values)
            loadBudgets()
        } catch (e: Exception) {
            Log.e("BudgetsActivity", "Error adding budget: ${e.message}")
            Toast.makeText(this, "Error adding budget", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateBudget(id: Long, category: String, limit: Double) {
        try {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put(DatabaseHelper.COLUMN_BUDGET_CATEGORY, getCategoryId(category))
                put(DatabaseHelper.COLUMN_BUDGET_LIMIT, limit)
            }

            db.update(
                DatabaseHelper.TABLE_BUDGETS,
                values,
                "${DatabaseHelper.COLUMN_ID} = ?",
                arrayOf(id.toString())
            )
            loadBudgets()
        } catch (e: Exception) {
            Log.e("BudgetsActivity", "Error updating budget: ${e.message}")
            Toast.makeText(this, "Error updating budget", Toast.LENGTH_SHORT).show()
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

        cursor?.use {
            if (it.moveToFirst()) {
                return it.getLong(it.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID))
            }
        }
        return -1
    }

    private fun deleteBudget(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Budget")
            .setMessage("Are you sure you want to delete this budget?")
            .setPositiveButton("Yes") { _, _ ->
                try {
                    val budget = budgets[position]
                    val db = dbHelper.writableDatabase
                    db.delete(
                        DatabaseHelper.TABLE_BUDGETS,
                        "${DatabaseHelper.COLUMN_ID} = ?",
                        arrayOf(budget.id.toString())
                    )
                    loadBudgets()
                    adapter.notifyDataSetChanged()
                } catch (e: Exception) {
                    Log.e("BudgetsActivity", "Error deleting budget: ${e.message}")
                    Toast.makeText(this, "Error deleting budget", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}

data class Budget(
    val id: Long,
    val category: String,
    val limit: Double,
    val spent: Double
)

class BudgetRecyclerAdapter(
    private val context: Context,
    private val budgets: List<Budget>,
    private val onBudgetAction: (Budget, Int, String) -> Unit
) :
    RecyclerView.Adapter<BudgetRecyclerAdapter.BudgetViewHolder>() {

    class BudgetViewHolder(itemView: View, private val adapter: BudgetRecyclerAdapter) : RecyclerView.ViewHolder(itemView) {
        val categoryTextView: TextView = itemView.findViewById(R.id.textViewCategory)
        val limitTextView: TextView = itemView.findViewById(R.id.textViewLimit)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBarBudget)
        val spentTextView: TextView = itemView.findViewById(R.id.textViewSpent)
        val remainingTextView: TextView = itemView.findViewById(R.id.textViewRemaining)

        init {
            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    showOptionsDialog(itemView.context, adapter.budgets[position], position, itemView, adapter.onBudgetAction)
                    true
                } else {
                    false
                }
            }
        }

        private fun showOptionsDialog(
            context: Context,
            budget: Budget,
            position: Int,
            itemView: View,
            onBudgetAction: (Budget, Int, String) -> Unit
        ) {
            val popupMenu = PopupMenu(context, itemView)
            popupMenu.menu.add("Edit")
            popupMenu.menu.add("Delete")
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.title) {
                    "Edit" -> onBudgetAction(budget, position, "edit")
                    "Delete" -> onBudgetAction(budget, position, "delete")
                }
                true
            }
            popupMenu.show()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_budget, parent, false)
        return BudgetViewHolder(itemView, this)
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        val currentBudget = budgets[position]
        holder.categoryTextView.text = "Category: ${currentBudget.category}"
        holder.limitTextView.text = "Limit: ${currentBudget.limit}"

        val spent = currentBudget.spent
        val remaining = currentBudget.limit - spent

        holder.spentTextView.text = "Spent: ${"%.2f".format(spent)}"
        holder.remainingTextView.text = "Remaining: ${"%.2f".format(remaining)}"

        val progress =
            if (currentBudget.limit != 0.0) (spent / currentBudget.limit * 100).toInt() else 0
        holder.progressBar.progress = progress
    }

    override fun getItemCount() = budgets.size
}