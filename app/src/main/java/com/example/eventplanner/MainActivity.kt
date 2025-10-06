package com.example.eventplanner

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.eventplanner.databinding.ActivityMainBinding
import com.example.eventplanner.models.*
import com.example.eventplanner.ui.InviteFragment
import com.example.eventplanner.utils.SettingsManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var db: FirebaseFirestore
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply saved theme and locale from code B
        SettingsManager.applySavedTheme(this)
        applySavedLocale()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        db = FirebaseFirestore.getInstance()

        setupWindowInsets()
        setupNavigation()
        setupClickListeners()
    }

    private fun applySavedLocale() {
        val code = com.example.eventplanner.utils.SettingsManager.getSavedLocale(this) ?: return
        val locale = java.util.Locale(code)
        java.util.Locale.setDefault(locale)
        val config = android.content.res.Configuration(resources.configuration)
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
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

        // Combined navigation destinations from both codes
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_dashboard,
                R.id.navigation_events,
                R.id.navigation_itinerary,
                R.id.navigation_guests,
                R.id.navigation_budget,
                R.id.navigation_map,
                R.id.navigation_chat,
                R.id.navigation_settings  // Added from code B
            ),
            binding.drawerLayout
        )

        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            val handled = try {
                navController.navigate(menuItem.itemId)
                true
            } catch (e: Exception) {
                Log.e(TAG, "Navigation error", e)
                false
            }
            binding.drawerLayout.closeDrawers()
            handled
        }
    }

    private fun setupClickListeners() {
        binding.btnSettings.setOnClickListener {
            // Navigate to settings from code B
            val navHostFragment = supportFragmentManager
                .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
            val navController = navHostFragment.navController
            navController.navigate(R.id.navigation_settings)
        }
    }

    // ------------------- Invites Navigation -------------------
    fun openInvitesFragment() {
        val fragment = InviteFragment()
        supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .addToBackStack(null)
            .commit()
    }

    // ------------------- Dialog Interfaces -------------------
    interface GuestDialogListener {
        fun onGuestCreated(guest: Guest)
    }

    interface EventDialogListener {
        fun onEventCreated(event: Event)
    }

    interface BudgetDialogListener {
        fun onBudgetCreated(budget: Budget)
    }

    interface ItineraryDialogListener {
        fun onItemCreated(item: ItineraryItem)
    }

    // ------------------- Guest Dialog -------------------
    // Simple version (from code B)
    fun showGuestDialog() {
        val dialog = AlertDialog.Builder(this)
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

    // Advanced version with Firebase (from code A)
    fun showGuestDialog(listener: GuestDialogListener) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_guest_creator, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        // Setup dialog elements
        setupGuestDialogElements(dialogView)

        val etFirstName = dialogView.findViewById<TextInputEditText>(R.id.et_first_name)
        val etLastName = dialogView.findViewById<TextInputEditText>(R.id.et_last_name)
        val etUsername = dialogView.findViewById<TextInputEditText>(R.id.et_username)
        val etPhone = dialogView.findViewById<TextInputEditText>(R.id.et_phone)
        val etPlusOne = dialogView.findViewById<TextInputEditText>(R.id.et_plus_one)
        val etDietary = dialogView.findViewById<TextInputEditText>(R.id.et_dietary_restrictions)
        val guestTypeSpinner = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.spinner_guest_type)

        dialogView.findViewById<View>(R.id.btn_create)?.setOnClickListener {
            try {
                val guest = Guest(
                    firstName = etFirstName?.text.toString().ifBlank { "Unknown" },
                    lastName = etLastName?.text.toString().ifBlank { "Unknown" },
                    username = etUsername?.text.toString().ifBlank { "guest_user" },
                    phone = etPhone?.text.toString().ifBlank { "0000000000" },
                    guestType = guestTypeSpinner?.text.toString().ifBlank { "Other" },
                    plusOne = etPlusOne?.text.toString().ifBlank { "None" },
                    dietaryRestrictions = etDietary?.text.toString().ifBlank { "None" },
                    status = "pending"
                )
                saveGuestToFirebase(guest)
                listener.onGuestCreated(guest)
                dialog.dismiss()
            } catch (e: Exception) {
                Log.e(TAG, "Error creating guest", e)
                Toast.makeText(
                    this,
                    "Failed to create guest: ${e.localizedMessage}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        dialogView.findViewById<View>(R.id.btn_cancel)?.setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<View>(R.id.btn_close)?.setOnClickListener { dialog.dismiss() }
    }

    // ------------------- Event Dialog -------------------
    fun showEventDialog(listener: EventDialogListener) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_event_creator, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.et_title)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.et_description)
        val etLocation = dialogView.findViewById<TextInputEditText>(R.id.et_location)
        val etStartDate = dialogView.findViewById<TextInputEditText>(R.id.et_start_date)
        val etEndDate = dialogView.findViewById<TextInputEditText>(R.id.et_end_date)
        val etBudget = dialogView.findViewById<TextInputEditText>(R.id.et_budget)
        val eventTypeSpinner = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.spinnerEventType)
        val etCustomCategory = dialogView.findViewById<TextInputEditText>(R.id.editCustomCategory)

        val eventTypes = arrayOf("Birthday", "Wedding", "Conference", "Party", "Meeting", "Other")
        eventTypeSpinner?.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, eventTypes))

        dialogView.findViewById<View>(R.id.btn_create)?.setOnClickListener {
            try {
                val event = com.google.firebase.events.Event(
                    type = eventTypeSpinner?.text.toString().ifBlank { "Other" },
                    customCategory = etCustomCategory?.text.toString().ifBlank { "" },
                    title = etTitle?.text.toString().ifBlank { "Untitled Event" },
                    description = etDescription?.text.toString().ifBlank { "" },
                    location = etLocation?.text.toString().ifBlank { "" },
                    startDate = etStartDate?.text.toString().ifBlank { "" },
                    endDate = etEndDate?.text.toString().ifBlank { "" },
                    budget = etBudget?.text.toString().ifBlank { "0" }
                )
                saveEventToFirebase(event)
                listener.onEventCreated(event)
                dialog.dismiss()
            } catch (e: Exception) {
                Log.e(TAG, "Error creating event", e)
                Toast.makeText(this, "Failed to create event: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }

        dialogView.findViewById<View>(R.id.btn_cancel)?.setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<View>(R.id.btn_close)?.setOnClickListener { dialog.dismiss() }
    }

    // ------------------- Budget Dialog -------------------
    // Simple version (from code B)
    fun showBudgetDialog() {
        val dialog = AlertDialog.Builder(this)
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

    // Advanced version with Firebase (from code A)
    fun showBudgetDialog(listener: BudgetDialogListener) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_budget_creator, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        // Setup dialog elements
        setupBudgetDialogElements(dialogView)

        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.et_expense_title)
        val etAmount = dialogView.findViewById<TextInputEditText>(R.id.et_amount)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.et_expense_description)
        val etNotes = dialogView.findViewById<TextInputEditText>(R.id.et_notes)
        val etDate = dialogView.findViewById<TextInputEditText>(R.id.et_date)

        val spinnerCurrency = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.spinner_currency)
        val spinnerCategory = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.spinner_expense_category)
        val spinnerPayment = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.spinner_payment_method)

        dialogView.findViewById<View>(R.id.btn_create)?.setOnClickListener {
            try {
                val amountText = etAmount?.text?.toString().orEmpty()
                val amount = amountText.toDoubleOrNull() ?: 0.0

                val budget = Budget(
                    id = "",
                    title = etTitle?.text.toString().ifBlank { "Untitled" },
                    amount = amount,
                    description = etDescription?.text.toString().ifBlank { "" },
                    notes = etNotes?.text.toString().ifBlank { "" },
                    currency = spinnerCurrency?.text.toString().ifBlank { "ZAR" },
                    category = spinnerCategory?.text.toString().ifBlank { "Other" },
                    paymentMethod = spinnerPayment?.text.toString().ifBlank { "Cash" },
                    date = etDate?.text.toString()
                )
                saveBudgetToFirebase(budget)
                listener.onBudgetCreated(budget)
                dialog.dismiss()
            } catch (e: Exception) {
                Log.e(TAG, "Error creating budget", e)
                Toast.makeText(this, "Failed to create budget: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }

        dialogView.findViewById<View>(R.id.btn_cancel)?.setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<View>(R.id.btn_close)?.setOnClickListener { dialog.dismiss() }
    }

    // ------------------- Itinerary Dialog -------------------
    // Simple version (from code B)
    fun showItineraryDialog() {
        val dialog = AlertDialog.Builder(this)
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

    // Advanced version with Firebase (from code A)
    fun showItineraryDialog(listener: ItineraryDialogListener) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_itinerary_creator, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        dialog.show()
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        // Setup dialog elements
        setupItineraryDialogElements(dialogView)

        val etTitle = dialogView.findViewById<TextInputEditText>(R.id.et_title)
        val etDescription = dialogView.findViewById<TextInputEditText>(R.id.et_description)
        val etLocation = dialogView.findViewById<TextInputEditText>(R.id.et_location)
        val etDate = dialogView.findViewById<TextInputEditText>(R.id.et_date)
        val etTime = dialogView.findViewById<TextInputEditText>(R.id.et_time)
        val etDuration = dialogView.findViewById<TextInputEditText>(R.id.et_duration)

        dialogView.findViewById<View>(R.id.btn_create)?.setOnClickListener {
            try {
                val item = ItineraryItem(
                    id = "",
                    title = etTitle?.text.toString().ifBlank { "Untitled" },
                    description = etDescription?.text.toString().ifBlank { "" },
                    location = etLocation?.text.toString().ifBlank { "" },
                    date = etDate?.text.toString().ifBlank { "" },
                    time = etTime?.text.toString().ifBlank { "" },
                    duration = etDuration?.text.toString().ifBlank { "" },
                    type = ""
                )
                saveItineraryToFirebase(item)
                listener.onItemCreated(item)
                dialog.dismiss()
            } catch (e: Exception) {
                Log.e(TAG, "Error creating itinerary item", e)
                Toast.makeText(this, "Failed to create item: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }

        dialogView.findViewById<View>(R.id.btn_cancel)?.setOnClickListener { dialog.dismiss() }
        dialogView.findViewById<View>(R.id.btn_close)?.setOnClickListener { dialog.dismiss() }
    }

    // ------------------- Dialog Element Setup Methods -------------------
    // Methods for AlertDialog (from code B)
    private fun setupItineraryDialogElements(dialog: AlertDialog) {
        // Setup Date Picker
        val dateEdit = dialog.findViewById<TextInputEditText>(R.id.et_date)
        dateEdit?.setOnClickListener {
            showDatePicker(dateEdit)
        }

        // Setup Time Picker
        val timeEdit = dialog.findViewById<TextInputEditText>(R.id.et_time)
        timeEdit?.setOnClickListener {
            showTimePicker(timeEdit)
        }
    }

    private fun setupGuestDialogElements(dialog: AlertDialog) {
        // Setup Guest Type Dropdown
        val guestTypeSpinner = dialog.findViewById<MaterialAutoCompleteTextView>(R.id.spinner_guest_type)
        val guestTypes = arrayOf(
            "Family",
            "Friend",
            "Colleague",
            "Business Contact",
            "VIP Guest",
            "Media",
            "Other"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, guestTypes)
        guestTypeSpinner?.setAdapter(adapter)
    }

    private fun setupBudgetDialogElements(dialog: AlertDialog) {
        // Setup Currency Dropdown
        val currencySpinner = dialog.findViewById<MaterialAutoCompleteTextView>(R.id.spinner_currency)
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

        val currencyAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, currencies)
        currencySpinner?.setAdapter(currencyAdapter)

        // Setup Expense Category Dropdown
        val categorySpinner = dialog.findViewById<MaterialAutoCompleteTextView>(R.id.spinner_expense_category)
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

        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        categorySpinner?.setAdapter(categoryAdapter)

        // Setup Payment Method Dropdown
        val paymentSpinner = dialog.findViewById<MaterialAutoCompleteTextView>(R.id.spinner_payment_method)
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

        val paymentAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, paymentMethods)
        paymentSpinner?.setAdapter(paymentAdapter)

        // Setup Date Picker
        val dateEdit = dialog.findViewById<TextInputEditText>(R.id.et_date)
        dateEdit?.setOnClickListener {
            showDatePicker(dateEdit)
        }
    }

    // Methods for View (from code A)
    private fun setupItineraryDialogElements(dialogView: View) {
        // Setup Date Picker
        val dateEdit = dialogView.findViewById<TextInputEditText>(R.id.et_date)
        dateEdit?.setOnClickListener {
            showDatePicker(dateEdit)
        }

        // Setup Time Picker
        val timeEdit = dialogView.findViewById<TextInputEditText>(R.id.et_time)
        timeEdit?.setOnClickListener {
            showTimePicker(timeEdit)
        }
    }

    private fun setupGuestDialogElements(dialogView: View) {
        // Setup Guest Type Dropdown
        val guestTypeSpinner = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.spinner_guest_type)
        val guestTypes = arrayOf(
            "Family",
            "Friend",
            "Colleague",
            "Business Contact",
            "VIP Guest",
            "Media",
            "Other"
        )

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, guestTypes)
        guestTypeSpinner?.setAdapter(adapter)
    }

    private fun setupBudgetDialogElements(dialogView: View) {
        // Setup Currency Dropdown
        val currencySpinner = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.spinner_currency)
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

        val currencyAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, currencies)
        currencySpinner?.setAdapter(currencyAdapter)

        // Setup Expense Category Dropdown
        val categorySpinner = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.spinner_expense_category)
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

        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        categorySpinner?.setAdapter(categoryAdapter)

        // Setup Payment Method Dropdown
        val paymentSpinner = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.spinner_payment_method)
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

        val paymentAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, paymentMethods)
        paymentSpinner?.setAdapter(paymentAdapter)

        // Setup Date Picker
        val dateEdit = dialogView.findViewById<TextInputEditText>(R.id.et_date)
        dateEdit?.setOnClickListener {
            showDatePicker(dateEdit)
        }
    }

    // ------------------- Firebase Save Methods -------------------
    private fun saveGuestToFirebase(guest: Guest) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Not signed in. Guest not saved.", Toast.LENGTH_SHORT).show()
            return
        }
        val uid = currentUser.uid
        val docRef = db.collection("guests").document(uid).collection("myGuests").document()
        guest.id = docRef.id
        docRef.set(guest)
            .addOnSuccessListener {
                Toast.makeText(this, "Guest saved!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "saveGuestToFirebase failed", e)
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveBudgetToFirebase(budget: Budget) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Not signed in. Budget not saved.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val uid = currentUser.uid
            val database = FirebaseDatabase.getInstance().getReference("budgets").child(uid)
            val newBudgetRef = database.push()
            budget.id = newBudgetRef.key ?: ""
            newBudgetRef.setValue(budget)
                .addOnSuccessListener { Toast.makeText(this, "Budget saved!", Toast.LENGTH_SHORT).show() }
                .addOnFailureListener { e ->
                    Log.e(TAG, "saveBudgetToFirebase failed", e)
                    Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "saveBudgetToFirebase exception", e)
            Toast.makeText(this, "Failed to save budget: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveItineraryToFirebase(item: ItineraryItem) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Not signed in. Itinerary not saved.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val database = FirebaseDatabase.getInstance().getReference("itineraries").child(currentUser.uid)
            val newRef = database.push()
            item.id = newRef.key ?: ""
            newRef.setValue(item)
                .addOnSuccessListener { Toast.makeText(this, "Itinerary saved!", Toast.LENGTH_SHORT).show() }
                .addOnFailureListener { e ->
                    Log.e(TAG, "saveItineraryToFirebase failed", e)
                    Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Log.e(TAG, "saveItineraryToFirebase exception", e)
            Toast.makeText(this, "Failed to save itinerary: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // ------------------- Date and Time Pickers -------------------
    private fun showDatePicker(editText: TextInputEditText) {
        try {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val date = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                    editText.setText(date)
                },
                year, month, day
            )
            datePickerDialog.show()
        } catch (e: Exception) {
            Log.e(TAG, "DatePicker error", e)
        }
    }

    private fun showTimePicker(editText: TextInputEditText) {
        try {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            val timePickerDialog = TimePickerDialog(
                this,
                { _, selectedHour, selectedMinute ->
                    val time = String.format("%02d:%02d", selectedHour, selectedMinute)
                    editText.setText(time)
                },
                hour, minute, true
            )
            timePickerDialog.show()
        } catch (e: Exception) {
            Log.e(TAG, "TimePicker error", e)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}