package com.example.eventplanner.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventplanner.MainActivity
import com.example.eventplanner.adapters.BudgetAdapter
import com.example.eventplanner.databinding.FragmentBudgetBinding
import com.example.eventplanner.models.Budget
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class BudgetFragment : Fragment() {

    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private lateinit var budgetAdapter: BudgetAdapter
    private val budgetsList = mutableListOf<Budget>()
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        database = FirebaseDatabase.getInstance()
            .getReference("budgets")
            .child(currentUser.uid)

        setupRecyclerView()
        listenForBudgetChanges()
        loadTotalBudgetFromFirebase()

        binding.btnAddBudget.setOnClickListener {
            (activity as? MainActivity)?.showBudgetDialog(object : MainActivity.BudgetDialogListener {
                override fun onBudgetCreated(budget: Budget) {
                    saveBudgetToFirebaseOnce(budget)
                }
            })
        }

        binding.etTotalBudget.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                updateBudgetOverview()
                saveTotalBudgetToFirebase()
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupRecyclerView() {
        budgetAdapter = BudgetAdapter(budgetsList)
        binding.rvBudgets.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBudgets.adapter = budgetAdapter
    }

    private fun saveBudgetToFirebaseOnce(budget: Budget) {
        if (budget.id.isEmpty()) {
            val newRef = database.push()
            budget.id = newRef.key ?: ""
            newRef.setValue(budget)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Expense added!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to save expense", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun listenForBudgetChanges() {
        database.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val budget = parseBudgetSnapshot(snapshot)
                if (budget != null && budget.id != "TOTAL_BUDGET") {
                    if (budgetsList.none { it.id == budget.id }) {
                        budgetsList.add(budget)
                        budgetAdapter.notifyItemInserted(budgetsList.size - 1)
                        updateBudgetOverview()
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val budget = parseBudgetSnapshot(snapshot)
                if (budget != null && budget.id != "TOTAL_BUDGET") {
                    val index = budgetsList.indexOfFirst { it.id == budget.id }
                    if (index != -1) {
                        budgetsList[index] = budget
                        budgetAdapter.notifyItemChanged(index)
                        updateBudgetOverview()
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val budget = parseBudgetSnapshot(snapshot)
                if (budget != null && budget.id != "TOTAL_BUDGET") {
                    val index = budgetsList.indexOfFirst { it.id == budget.id }
                    if (index != -1) {
                        budgetsList.removeAt(index)
                        budgetAdapter.notifyItemRemoved(index)
                        updateBudgetOverview()
                    }
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to fetch budgets: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    /** Parse snapshot safely to handle Long → Double conversion */
    private fun parseBudgetSnapshot(snapshot: DataSnapshot): Budget? {
        val map = snapshot.value as? Map<*, *> ?: return null
        val amountValue = map["amount"]
        val amountDouble = when (amountValue) {
            is Long -> amountValue.toDouble()
            is Double -> amountValue
            else -> 0.0
        }

        return Budget(
            id = map["id"] as? String ?: "",
            title = map["title"] as? String ?: "",
            description = map["description"] as? String ?: "",
            amount = amountDouble,
            currency = map["currency"] as? String ?: "",
            category = map["category"] as? String ?: "",
            date = map["date"] as? String ?: "",
            paymentMethod = map["paymentMethod"] as? String ?: "",
            notes = map["notes"] as? String ?: ""
        )
    }

    /** Update progress and total expenses */
    private fun updateBudgetOverview() {
        val totalBudget = binding.etTotalBudget.text.toString().toDoubleOrNull() ?: 0.0
        val totalExpenses = budgetsList.sumOf { it.amount }

        binding.tvTotalExpenses.text = "R$totalExpenses"
        val progress = if (totalBudget > 0) ((totalExpenses / totalBudget) * 100).toInt().coerceAtMost(100) else 0
        binding.progressBudget.progress = progress
    }

    /** Save total budget as a simple Double per user */
    private fun saveTotalBudgetToFirebase() {
        val currentUser = auth.currentUser ?: return
        val totalBudgetValue = binding.etTotalBudget.text.toString().toDoubleOrNull() ?: 0.0
        database.child("totalBudget").setValue(totalBudgetValue)
    }

    /** Load total budget from Firebase */
    private fun loadTotalBudgetFromFirebase() {
        database.child("totalBudget").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val totalBudgetValue = snapshot.getValue(Double::class.java) ?: 0.0
                binding.etTotalBudget.setText(totalBudgetValue.toString())
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}