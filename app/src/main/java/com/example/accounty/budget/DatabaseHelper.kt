package com.example.accounty.budget

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "BudgetTier.db"
        private const val DATABASE_VERSION = 1

        // Table names
        const val TABLE_TRANSACTIONS = "transactions"
        const val TABLE_BUDGETS = "budgets"
        const val TABLE_TAGS = "tags"
        const val TABLE_CATEGORIES = "categories"

        // Common column names
        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "name"

        // Transactions table column names
        const val COLUMN_TRANSACTION_AMOUNT = "amount"
        const val COLUMN_TRANSACTION_CATEGORY = "category"
        const val COLUMN_TRANSACTION_TAG = "tag"
        const val COLUMN_TRANSACTION_DATE = "date"
        const val COLUMN_TRANSACTION_NOTE = "note"

        // Budgets table column names
        const val COLUMN_BUDGET_CATEGORY = "category"
        const val COLUMN_BUDGET_LIMIT = "budget_limit" // renamed from "limit" to "budget_limit"

        // Tags table column names (using common names)

        // Categories table column names (using common names)
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create transactions table
        val createTransactionsTable = """
            CREATE TABLE $TABLE_TRANSACTIONS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TRANSACTION_AMOUNT REAL NOT NULL,
                $COLUMN_TRANSACTION_CATEGORY INTEGER NOT NULL,
                $COLUMN_TRANSACTION_TAG INTEGER,
                $COLUMN_TRANSACTION_DATE TEXT NOT NULL,
                $COLUMN_TRANSACTION_NOTE TEXT,
                FOREIGN KEY ($COLUMN_TRANSACTION_CATEGORY) REFERENCES $TABLE_CATEGORIES($COLUMN_ID),
                FOREIGN KEY ($COLUMN_TRANSACTION_TAG) REFERENCES $TABLE_TAGS($COLUMN_ID)
            )
        """.trimIndent()
        db.execSQL(createTransactionsTable)

        // Create budgets table
        val createBudgetsTable = """
            CREATE TABLE $TABLE_BUDGETS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_BUDGET_CATEGORY INTEGER NOT NULL,
                $COLUMN_BUDGET_LIMIT REAL NOT NULL,
                FOREIGN KEY ($COLUMN_BUDGET_CATEGORY) REFERENCES $TABLE_CATEGORIES($COLUMN_ID)
            )
        """.trimIndent()
        db.execSQL(createBudgetsTable)

        // Create tags table
        val createTagsTable = """
            CREATE TABLE $TABLE_TAGS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL UNIQUE
            )
        """.trimIndent()
        db.execSQL(createTagsTable)

        // Create categories table
        val createCategoriesTable = """
            CREATE TABLE $TABLE_CATEGORIES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL UNIQUE
            )
        """.trimIndent()
        db.execSQL(createCategoriesTable)

        // Insert sample data
        insertSampleData(db)
    }

    private fun insertSampleData(db: SQLiteDatabase) {
        try {
            // Sample categories
            val foodValues = ContentValues().apply {
                put(COLUMN_NAME, "Food")
            }
            db.insert(TABLE_CATEGORIES, null, foodValues)

            val transportationValues = ContentValues().apply {
                put(COLUMN_NAME, "Transportation")
            }
            db.insert(TABLE_CATEGORIES, null, transportationValues)

            val entertainmentValues = ContentValues().apply {
                put(COLUMN_NAME, "Entertainment")
            }
            db.insert(TABLE_CATEGORIES, null, entertainmentValues)

            // Sample tags
            val groceriesValues = ContentValues().apply {
                put(COLUMN_NAME, "Groceries")
            }
            db.insert(TABLE_TAGS, null, groceriesValues)

            val gasValues = ContentValues().apply {
                put(COLUMN_NAME, "Gas")
            }
            db.insert(TABLE_TAGS, null, gasValues)

        } catch (e: Exception) {
            Log.e("DatabaseHelper", "Error inserting sample data: ${e.message}")
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TRANSACTIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BUDGETS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TAGS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CATEGORIES")
        onCreate(db)
    }
    fun getTotalSpentForCategory(categoryId: Long): Double {
        val db = readableDatabase
        var total = 0.0
        val cursor = db.rawQuery(
            "SELECT SUM(${COLUMN_TRANSACTION_AMOUNT}) FROM $TABLE_TRANSACTIONS WHERE $COLUMN_TRANSACTION_CATEGORY = ?",
            arrayOf(categoryId.toString())
        )
        cursor?.use {
            if (it.moveToFirst()) {
                total = it.getDouble(0)
            }
        }
        return total
    }


}
