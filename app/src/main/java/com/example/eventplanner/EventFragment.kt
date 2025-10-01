package com.example.eventplanner

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.eventplanner.databinding.FragmentEventBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import androidx.appcompat.app.AlertDialog
import android.widget.ArrayAdapter
import java.util.Calendar

class EventFragment : Fragment() {

    private var _binding: FragmentEventBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.fabNewEvent.setOnClickListener {
            showEventCreationDialog()
        }
    }

    private fun showEventCreationDialog() {
        val dialog = AlertDialog.Builder(requireContext())
            .setView(R.layout.dialog_event_creator)
            .create()

        dialog.show()
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        setupDialogElements(dialog)

        dialog.findViewById<View>(R.id.btn_close)?.setOnClickListener { dialog.dismiss() }
        dialog.findViewById<View>(R.id.btn_cancel)?.setOnClickListener { dialog.dismiss() }
        dialog.findViewById<View>(R.id.btn_create)?.setOnClickListener {
            Snackbar.make(binding.root, "Event creation functionality coming soon!", Snackbar.LENGTH_SHORT).show()
            dialog.dismiss()
        }
    }

    private fun setupDialogElements(dialog: AlertDialog) {
        val eventTypeSpinner =
            dialog.findViewById<MaterialAutoCompleteTextView>(R.id.spinnerEventType)

        val eventCategories = arrayOf(
            "Birthday Party", "Wedding", "Corporate Event", "Conference", "Workshop",
            "Concert", "Exhibition", "Sports Event", "Dinner Party", "Graduation",
            "Anniversary", "Holiday Celebration", "Other"
        )

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, eventCategories)
        eventTypeSpinner?.setAdapter(adapter)

        val startDateEdit = dialog.findViewById<TextInputEditText>(R.id.et_start_date)
        val endDateEdit = dialog.findViewById<TextInputEditText>(R.id.et_end_date)

        startDateEdit?.setOnClickListener { showDatePicker(startDateEdit) }
        endDateEdit?.setOnClickListener { showDatePicker(endDateEdit) }
    }

    private fun showDatePicker(editText: TextInputEditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                val date = String.format("%02d/%02d/%04d", selectedDay, selectedMonth + 1, selectedYear)
                editText.setText(date)
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
