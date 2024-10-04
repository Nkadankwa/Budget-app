package com.example.accounty.budget

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.TextView
import com.example.accounty.R

class BudgetAdapter(context: Context, private val budgets: List<Budget>, private val dbHelper: DatabaseHelper) :
    ArrayAdapter<Budget>(context, R.layout.list_item_budget, budgets) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.list_item_budget, parent, false)

        val budget = getItem(position)

        val categoryTextView = view.findViewById<TextView>(R.id.textViewCategory)
        val limitTextView = view.findViewById<TextView>(R.id.textViewLimit)
        val progressBar = view.findViewById<ProgressBar>(R.id.progressBarBudget)

        categoryTextView.text = budget?.category
        limitTextView.text = "Remaining: ${"%.2f".format(budget?.limit?.minus(budget?.spent ?: 0.0))} / ${budget?.limit}"

        val percentage = if (budget != null && budget.limit > 0) {
            ((budget.spent / budget.limit) * 100).toInt().coerceAtMost(100)
        } else 0

        progressBar.progress = percentage

        return view
    }
}
