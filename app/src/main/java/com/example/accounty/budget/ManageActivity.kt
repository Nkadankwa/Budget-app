package com.example.accounty.budget

import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.tabs.TabLayout
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.accounty.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ManageActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var listView: ListView
    private lateinit var items: MutableList<String>
    private lateinit var adapter: ArrayAdapter<String>
    private lateinit var dbHelper: DatabaseHelper
    private var currentTab = 0 // 0 for categories, 1 for tags

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage)

        tabLayout = findViewById(R.id.tabLayout)
        listView = findViewById(R.id.listView)
        items = mutableListOf()
        dbHelper = DatabaseHelper(this)

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
        listView.adapter = adapter

        loadItems()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                loadItems()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        findViewById<Button>(R.id.buttonAdd).setOnClickListener {
            showAddDialog()
        }

        listView.setOnItemLongClickListener { _, _, position, _ ->
            deleteItem(position)
            true
        }
    }

    private fun loadItems() {
        items.clear()
        val db = dbHelper.readableDatabase
        val tableName = if (currentTab == 0) DatabaseHelper.TABLE_CATEGORIES else DatabaseHelper.TABLE_TAGS
        val columnName = DatabaseHelper.COLUMN_NAME

        val cursor: Cursor? = db.query(
            tableName,
            arrayOf(DatabaseHelper.COLUMN_ID, columnName),
            null, null, null, null, null
        )

        cursor?.use {
            val nameColumnIndex = it.getColumnIndex(columnName)
            while (it.moveToNext()) {
                items.add(it.getString(nameColumnIndex))
            }
        }
        cursor?.close()
        adapter.notifyDataSetChanged()
    }

    private fun showAddDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add ${if (currentTab == 0) "Category" else "Tag"}")

        val input = EditText(this)
        builder.setView(input)

        builder.setPositiveButton("Add") { _, _ ->
            val text = input.text.toString().trim()
            if (TextUtils.isEmpty(text)) {
                input.error = "Name is required"
                return@setPositiveButton
            }
            if (text.isNotEmpty()) {
                addItem(text)
            }
        }

        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun addItem(name: String) {
        try {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put(DatabaseHelper.COLUMN_NAME, name)
            }

            val tableName = if (currentTab == 0) DatabaseHelper.TABLE_CATEGORIES else DatabaseHelper.TABLE_TAGS
            db.insert(tableName, null, values)
            loadItems()
        } catch (e: Exception) {
            Log.e("ManageActivity", "Error adding item: ${e.message}")
            Toast.makeText(this, "Error adding item", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteItem(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete")
            .setMessage("Are you sure you want to delete this item?")
            .setPositiveButton("Yes") { _, _ ->
                try {
                    val db = dbHelper.writableDatabase
                    val tableName = if (currentTab == 0) DatabaseHelper.TABLE_CATEGORIES else DatabaseHelper.TABLE_TAGS
                    val columnName = DatabaseHelper.COLUMN_NAME
                    val itemToDelete = items[position]

                    db.delete(
                        tableName,
                        "$columnName = ?",
                        arrayOf(itemToDelete)
                    )
                    loadItems()
                } catch (e: Exception) {
                    Log.e("ManageActivity", "Error deleting item: ${e.message}")
                    Toast.makeText(this, "Error deleting item", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}