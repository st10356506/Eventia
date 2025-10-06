package com.example.eventplanner.fragments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventplanner.MainActivity
import com.example.eventplanner.adapters.ItineraryAdapter
import com.example.eventplanner.databinding.FragmentItineraryBinding
import com.example.eventplanner.models.ItineraryItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.util.*

class ItineraryFragment : Fragment() {

    private var _binding: FragmentItineraryBinding? = null
    private val binding get() = _binding!!

    private lateinit var database: DatabaseReference
    private lateinit var adapter: ItineraryAdapter
    private val itemsList = mutableListOf<ItineraryItem>()
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItineraryBinding.inflate(inflater, container, false)
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
            .getReference("itineraries")
            .child(currentUser.uid)

        setupRecyclerView()
        listenForItineraryChanges()

        binding.btnAddItinerary.setOnClickListener {
            (activity as? MainActivity)?.showItineraryDialog(object :
                MainActivity.ItineraryDialogListener {
                override fun onItemCreated(item: ItineraryItem) {
                    showDateTimePicker(item)
                }
            })
        }
    }

    private fun setupRecyclerView() {
        adapter = ItineraryAdapter(itemsList) { item ->
            deleteItineraryItem(item)
        }
        binding.rvItineraryTimeline.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItineraryTimeline.adapter = adapter
    }

    private fun showDateTimePicker(item: ItineraryItem) {
        val calendar = Calendar.getInstance()

        val datePicker = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                item.date = selectedDate

                val timePicker = TimePickerDialog(
                    requireContext(),
                    { _, hourOfDay, minute ->
                        val selectedTime = String.format("%02d:%02d", hourOfDay, minute)
                        item.time = selectedTime
                        saveItemToFirebase(item)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                )
                timePicker.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun saveItemToFirebase(item: ItineraryItem) {
        if (item.id.isEmpty()) {
            val newRef = database.push()
            item.id = newRef.key ?: ""
            newRef.setValue(item)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Itinerary item added!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to save item", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun deleteItineraryItem(item: ItineraryItem) {
        database.child(item.id).removeValue()
            .addOnSuccessListener {
                val index = itemsList.indexOfFirst { it.id == item.id }
                if (index != -1) {
                    itemsList.removeAt(index)
                    adapter.notifyItemRemoved(index)
                }
                Toast.makeText(requireContext(), "Item deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to delete item", Toast.LENGTH_SHORT).show()
            }
    }

    private fun listenForItineraryChanges() {
        database.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val item = snapshot.getValue(ItineraryItem::class.java)
                item?.let {
                    if (itemsList.none { it.id == item.id }) {
                        itemsList.add(item)
                        adapter.notifyItemInserted(itemsList.size - 1)
                    }
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val item = snapshot.getValue(ItineraryItem::class.java)
                item?.let {
                    val index = itemsList.indexOfFirst { it.id == item.id }
                    if (index != -1) {
                        itemsList[index] = item
                        adapter.notifyItemChanged(index)
                    }
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val item = snapshot.getValue(ItineraryItem::class.java)
                item?.let {
                    val index = itemsList.indexOfFirst { it.id == item.id }
                    if (index != -1) {
                        itemsList.removeAt(index)
                        adapter.notifyItemRemoved(index)
                    }
                }
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to fetch items: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}