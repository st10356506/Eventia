package com.example.eventplanner

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.eventplanner.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import android.view.View
import android.view.ViewGroup

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        setupWindowInsets()
        setupNavigation()
        setupClickListeners()
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_dashboard,
                R.id.navigation_events,
                R.id.navigation_itinerary,
                R.id.navigation_guests,
                R.id.navigation_budget,
                R.id.navigation_map,
                R.id.navigation_chat
            ),
            binding.drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            navController.navigate(menuItem.itemId)
            binding.drawerLayout.closeDrawers()
            true
        }
    }

    private fun setupClickListeners() {
        binding.btnSettings.setOnClickListener {
            Snackbar.make(
                binding.root,
                "Settings coming soon!",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    // Itinerary Dialog
    fun showItineraryDialog() {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(R.layout.dialog_itinerary_creator)
            .create()
        
        dialog.show()
        
        // Set dialog width to match parent
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        // Setup dialog elements
        setupItineraryDialogElements(dialog)
        
        // Setup dialog button click listeners
        dialog.findViewById<View>(R.id.btn_close)?.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.findViewById<View>(R.id.btn_cancel)?.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.findViewById<View>(R.id.btn_create)?.setOnClickListener {
            Snackbar.make(
                binding.root,
                "Itinerary item created successfully!",
                Snackbar.LENGTH_SHORT
            ).show()
            dialog.dismiss()
        }
    }
    
    // Guest Dialog
    fun showGuestDialog() {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(R.layout.dialog_guest_creator)
            .create()
        
        dialog.show()
        
        // Set dialog width to match parent
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        // Setup dialog elements
        setupGuestDialogElements(dialog)
        
        // Setup dialog button click listeners
        dialog.findViewById<View>(R.id.btn_close)?.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.findViewById<View>(R.id.btn_cancel)?.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.findViewById<View>(R.id.btn_create)?.setOnClickListener {
            Snackbar.make(
                binding.root,
                "Guest invited successfully!",
                Snackbar.LENGTH_SHORT
            ).show()
            dialog.dismiss()
        }
    }
    
    // Budget Dialog
    fun showBudgetDialog() {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(R.layout.dialog_budget_creator)
            .create()
        
        dialog.show()
        
        // Set dialog width to match parent
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        
        // Setup dialog elements
        setupBudgetDialogElements(dialog)
        
        // Setup dialog button click listeners
        dialog.findViewById<View>(R.id.btn_close)?.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.findViewById<View>(R.id.btn_cancel)?.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.findViewById<View>(R.id.btn_create)?.setOnClickListener {
            Snackbar.make(
                binding.root,
                "Expense added successfully!",
                Snackbar.LENGTH_SHORT
            ).show()
            dialog.dismiss()
        }
    }
    
    private fun setupItineraryDialogElements(dialog: androidx.appcompat.app.AlertDialog) {
        // Setup Date Picker
        val dateEdit = dialog.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_date)
        dateEdit?.setOnClickListener {
            showDatePicker(dateEdit)
        }
        
        // Setup Time Picker
        val timeEdit = dialog.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_time)
        timeEdit?.setOnClickListener {
            showTimePicker(timeEdit)
        }
    }
    
    private fun setupGuestDialogElements(dialog: androidx.appcompat.app.AlertDialog) {
        // Setup Guest Type Dropdown
        val guestTypeSpinner = dialog.findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.spinner_guest_type)
        val guestTypes = arrayOf(
            "Family",
            "Friend",
            "Colleague",
            "Business Contact",
            "VIP Guest",
            "Media",
            "Other"
        )
        
        val adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, guestTypes)
        guestTypeSpinner?.setAdapter(adapter)
    }
    
    private fun setupBudgetDialogElements(dialog: androidx.appcompat.app.AlertDialog) {
        // Setup Currency Dropdown
        val currencySpinner = dialog.findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.spinner_currency)
        val currencies = arrayOf(
            "USD ($)",
            "EUR (€)",
            "GBP (£)",
            "JPY (¥)",
            "CAD (C$)",
            "AUD (A$)",
            "CHF (CHF)",
            "CNY (¥)",
            "INR (₹)",
            "BRL (R$)",
            "ZAR (R)"
        )
        
        val currencyAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, currencies)
        currencySpinner?.setAdapter(currencyAdapter)
        
        // Setup Expense Category Dropdown
        val categorySpinner = dialog.findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.spinner_expense_category)
        val categories = arrayOf(
            "Venue & Location",
            "Catering & Food",
            "Entertainment",
            "Transportation",
            "Accommodation",
            "Decorations",
            "Photography & Video",
            "Marketing & Promotion",
            "Staff & Services",
            "Equipment & Supplies",
            "Insurance & Permits",
            "Miscellaneous"
        )
        
        val categoryAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        categorySpinner?.setAdapter(categoryAdapter)
        
        // Setup Payment Method Dropdown
        val paymentSpinner = dialog.findViewById<com.google.android.material.textfield.MaterialAutoCompleteTextView>(R.id.spinner_payment_method)
        val paymentMethods = arrayOf(
            "Credit Card",
            "Debit Card",
            "Cash",
            "Bank Transfer",
            "PayPal",
            "Check",
            "Mobile Payment",
            "Other"
        )
        
        val paymentAdapter = android.widget.ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, paymentMethods)
        paymentSpinner?.setAdapter(paymentAdapter)
        
        // Setup Date Picker
        val dateEdit = dialog.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.et_date)
        dateEdit?.setOnClickListener {
            showDatePicker(dateEdit)
        }
    }
    
    private fun showDatePicker(editText: com.google.android.material.textfield.TextInputEditText) {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH)
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        
        val datePickerDialog = android.app.DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val date = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                editText.setText(date)
            },
            year, month, day
        )
        datePickerDialog.show()
    }
    
    private fun showTimePicker(editText: com.google.android.material.textfield.TextInputEditText) {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = calendar.get(java.util.Calendar.MINUTE)
        
        val timePickerDialog = android.app.TimePickerDialog(
            this,
            { _, selectedHour, selectedMinute ->
                val time = String.format("%02d:%02d", selectedHour, selectedMinute)
                editText.setText(time)
            },
            hour, minute, true
        )
        timePickerDialog.show()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
