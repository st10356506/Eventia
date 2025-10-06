package com.example.eventplanner.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.eventplanner.databinding.ItemExpenseBinding
import com.example.eventplanner.models.Budget

class BudgetAdapter(private val budgets: MutableList<Budget>) :
    RecyclerView.Adapter<BudgetAdapter.BudgetViewHolder>() {

    inner class BudgetViewHolder(val binding: ItemExpenseBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BudgetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        val budget = budgets[position]
        holder.binding.tvExpenseTitle.text = budget.title
        holder.binding.tvExpenseAmount.text = "${budget.currency}${budget.amount}"
        holder.binding.tvExpenseCategory.text = budget.category
        holder.binding.tvExpenseDate.text = budget.date
        holder.binding.tvExpenseNotes.text = budget.notes
    }

    override fun getItemCount(): Int = budgets.size

    fun addBudget(budget: Budget) {
        budgets.add(budget)
        notifyItemInserted(budgets.size - 1)
    }
}