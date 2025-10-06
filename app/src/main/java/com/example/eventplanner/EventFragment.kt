package com.example.eventplanner

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.eventplanner.adapters.EventAdapter
import com.example.eventplanner.databinding.FragmentEventBinding
import com.example.eventplanner.databinding.DialogLocationInputBinding
import com.example.eventplanner.databinding.DialogCreateEventBinding
import com.example.eventplanner.databinding.DialogEventFilterBinding
import com.example.eventplanner.network.*
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import com.example.eventplanner.utils.LocationUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EventFragment : Fragment() {

    private var _binding: FragmentEventBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var eventAdapter: EventAdapter
    private var allEvents = mutableListOf<UnifiedEvent>()
    private var currentClassification: String? = null
    private var currentKeyword: String? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            getUserLocationAndFetchEvents()
        } else {
            Snackbar.make(requireView(), "Location permission denied. Enter a location manually.", Snackbar.LENGTH_LONG).show()
            showLocationInputDialog()
        }
    }

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

        setupRecyclerView()
        setupLocationClient()
        setupClickListeners()
        loadUserCreatedEvents()
        // Show location dialog to start finding events
        showLocationInputDialog()
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter(allEvents) { event ->
            Toast.makeText(requireContext(), "Clicked: ${event.title}", Toast.LENGTH_SHORT).show()
        }
        binding.rvEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEvents.adapter = eventAdapter
    }

    private fun setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
    }

    private fun setupClickListeners() {
        binding.fabNewEvent.setOnClickListener {
            showCreateEventDialog()
        }

        binding.btnFilterEvents.setOnClickListener {
            showEventFilterDialog()
        }

        binding.btnRefreshEvents.setOnClickListener {
            loadUserEvents()
        }
    }

    private fun loadUserEvents() {
        if (::fusedLocationClient.isInitialized) {
            getUserLocationAndFetchEvents()
        } else {
            showLocationInputDialog()
        }
    }

    private fun showLocationInputDialog() {
        val dialogBinding = DialogLocationInputBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancelLocation.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnUseCurrentLocation.setOnClickListener {
            dialog.dismiss()
            allEvents.clear()
            eventAdapter.updateEvents(allEvents)
            getUserLocationAndFetchEvents()
        }

        dialogBinding.btnConfirmLocation.setOnClickListener {
            val locationInput = dialogBinding.etLocationInput.text.toString().trim()
            if (locationInput.isNotEmpty()) {
                dialog.dismiss()
                allEvents.clear()
                eventAdapter.updateEvents(allEvents)
                fetchEventsFromEventbriteByLocation(locationInput)
            } else {
                Toast.makeText(requireContext(), "Please enter a location", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showCreateEventDialog() {
        val dialogBinding = DialogCreateEventBinding.inflate(layoutInflater)

        // --- DATE PICKER SETUP ---
        dialogBinding.etEventDate.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH)
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

            val datePicker = android.app.DatePickerDialog(requireContext(),
                { _, selectedYear, selectedMonth, selectedDay ->
                    val formattedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
                    dialogBinding.etEventDate.setText(formattedDate)
                }, year, month, day
            )

            datePicker.show()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancelEvent.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnCreateEvent.setOnClickListener {
            val title = dialogBinding.etEventTitle.text.toString().trim()
            val description = dialogBinding.etEventDescription.text.toString().trim()
            val type = dialogBinding.etEventType.text.toString().trim()
            val startDate = dialogBinding.etEventDate.text.toString().trim()
            val location = dialogBinding.etEventLocation.text.toString().trim()

            if (title.isNotEmpty() && type.isNotEmpty() && startDate.isNotEmpty()) {
                dialog.dismiss()
                createNewEvent(title, description, type, startDate, location)
            } else {
                Toast.makeText(requireContext(), "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun createNewEvent(title: String, description: String, type: String, startDate: String, location: String) {
        val newEvent = UnifiedEvent(
            id = "user_${System.currentTimeMillis()}",
            title = title,
            description = description,
            type = type,
            startDate = startDate,
            location = if (location.isNotEmpty()) location else null,
            source = "user"
        )

        allEvents.add(0, newEvent)
        eventAdapter.updateEvents(allEvents)
        pushEventToApi(newEvent)
        Toast.makeText(requireContext(), "Event created successfully!", Toast.LENGTH_SHORT).show()
    }

    private fun loadUserCreatedEvents() {
        lifecycleScope.launch(Dispatchers.IO) {
            val response = RetrofitClient.eventiaApi.getAllEvents()
            if (response.isSuccessful) {
                val userEvents = response.body() ?: emptyList()
                withContext(Dispatchers.Main) {
                    allEvents.addAll(0, userEvents)
                    eventAdapter.updateEvents(allEvents)
                }
            }
        }
    }

    private fun showEventFilterDialog() {
        val dialogBinding = DialogEventFilterBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        currentClassification?.let { classification ->
            when (classification.lowercase()) {
                "music" -> dialogBinding.chipMusic.isChecked = true
                "sports" -> dialogBinding.chipSports.isChecked = true
                "arts" -> dialogBinding.chipArts.isChecked = true
                "film" -> dialogBinding.chipFilm.isChecked = true
                "miscellaneous" -> dialogBinding.chipMiscellaneous.isChecked = true
            }
        }
        currentKeyword?.let { keyword ->
            dialogBinding.etSearchKeyword.setText(keyword)
        }

        dialogBinding.btnCancelFilter.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnApplyFilter.setOnClickListener {
            val selectedClassification = when {
                dialogBinding.chipMusic.isChecked -> "Music"
                dialogBinding.chipSports.isChecked -> "Sports"
                dialogBinding.chipArts.isChecked -> "Arts"
                dialogBinding.chipFilm.isChecked -> "Film"
                dialogBinding.chipMiscellaneous.isChecked -> "Miscellaneous"
                else -> null
            }

            val keyword = dialogBinding.etSearchKeyword.text.toString().trim()

            currentClassification = selectedClassification
            currentKeyword = if (keyword.isNotEmpty()) keyword else null

            dialog.dismiss()
            allEvents.clear()
            eventAdapter.updateEvents(allEvents)

            if (::fusedLocationClient.isInitialized) {
                getUserLocationAndFetchEvents()
            } else {
                showLocationInputDialog()
            }
        }

        dialogBinding.chipClear.setOnClickListener {
            dialogBinding.chipMusic.isChecked = false
            dialogBinding.chipSports.isChecked = false
            dialogBinding.chipArts.isChecked = false
            dialogBinding.chipFilm.isChecked = false
            dialogBinding.chipMiscellaneous.isChecked = false
            dialogBinding.etSearchKeyword.setText("")
        }

        dialog.show()
    }

    private fun getUserLocationAndFetchEvents() {
        val fineGranted = ActivityCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ActivityCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                fetchEventsFromEventbrite(it.latitude, it.longitude)
            } ?: Snackbar.make(binding.root, "Could not get location", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun fetchEventsFromEventbrite(lat: Double, lng: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val latlong = "$lat,$lng"
                val response = RetrofitClient.ticketmasterApi.searchEvents(
                    apiKey = "D5nbt3rsOCggZWiebPysFS6oLaiseKDy",
                    latlong = latlong,
                    radius = "25",
                    keyword = currentKeyword,
                    classification = currentClassification,
                    size = "20",
                    sort = "date,asc"
                )
                if (response.isSuccessful) {
                    val ticketmasterEvents = response.body()?._embedded?.events ?: emptyList()
                    val unifiedEvents = ticketmasterEvents.map { ticketmasterEvent ->
                        convertTicketmasterToUnified(ticketmasterEvent)
                    }
                    
                    withContext(Dispatchers.Main) {
                        // Add real events to the list
                        allEvents.addAll(unifiedEvents)
                        eventAdapter.updateEvents(allEvents)
                        
                        if (unifiedEvents.isNotEmpty()) {
                            Snackbar.make(
                                binding.root,
                                "Found ${unifiedEvents.size} real events nearby!",
                                Snackbar.LENGTH_LONG
                            ).show()
                        } else {
                            Snackbar.make(
                                binding.root,
                                "No events found near you.",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    val errorText = try { response.errorBody()?.string() } catch (_: Exception) { null }
                    withContext(Dispatchers.Main) {
                        Snackbar.make(
                            binding.root,
                            "Ticketmaster error ${response.code()}${if (!errorText.isNullOrBlank()) ": $errorText" else ""}",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(
                        binding.root,
                        "Failed to fetch events: ${e.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun fetchEventsFromEventbriteByLocation(location: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Try to get coordinates for the location first
                val resolved = try { 
                    LocationUtils.getLocationFromAddress(requireContext(), location) 
                } catch (e: Exception) { 
                    null 
                }
                
                val response = if (resolved != null) {
                    // Use coordinates if available
                    RetrofitClient.ticketmasterApi.searchEvents(
                        apiKey = "D5nbt3rsOCggZWiebPysFS6oLaiseKDy",
                        latlong = "${resolved.latitude},${resolved.longitude}",
                        radius = "25",
                        keyword = currentKeyword,
                        classification = currentClassification,
                        size = "20",
                        sort = "date,asc"
                    )
                } else {
                    // Fallback to keyword search
                    RetrofitClient.ticketmasterApi.searchEvents(
                        apiKey = "D5nbt3rsOCggZWiebPysFS6oLaiseKDy",
                        latlong = null,
                        radius = "25",
                        keyword = currentKeyword ?: location,
                        classification = currentClassification,
                        size = "20",
                        sort = "date,asc"
                    )
                }
                
                if (response.isSuccessful) {
                    val ticketmasterEvents = response.body()?._embedded?.events ?: emptyList()
                    val unifiedEvents = ticketmasterEvents.map { ticketmasterEvent ->
                        convertTicketmasterToUnified(ticketmasterEvent)
                    }
                    
                    withContext(Dispatchers.Main) {
                        // Add real events to the list
                        allEvents.addAll(unifiedEvents)
                        eventAdapter.updateEvents(allEvents)
                        
                        if (unifiedEvents.isNotEmpty()) {
                            Snackbar.make(
                                binding.root,
                                "Found events in $location!",
                                Snackbar.LENGTH_LONG
                            ).show()
                        } else {
                            Snackbar.make(
                                binding.root,
                                "No events found in $location.",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    val errorText = try { response.errorBody()?.string() } catch (_: Exception) { null }
                    withContext(Dispatchers.Main) {
                        Snackbar.make(
                            binding.root,
                            "Failed to fetch events for $location: ${response.code()}${if (!errorText.isNullOrBlank()) ": $errorText" else ""}",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(
                        binding.root,
                        "Error fetching events for $location: ${e.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun convertTicketmasterToUnified(ticketmasterEvent: TicketmasterEvent): UnifiedEvent {
        val venue = ticketmasterEvent._embedded?.venues?.firstOrNull()
        val attraction = ticketmasterEvent._embedded?.attractions?.firstOrNull()
        val classification = ticketmasterEvent.classifications?.firstOrNull()
        val priceRange = ticketmasterEvent.priceRanges?.firstOrNull()
        
        val location = venue?.let { v ->
            val city = v.city?.name ?: ""
            val state = v.state?.name ?: ""
            val venueName = v.name
            "$venueName${if (city.isNotEmpty()) ", $city" else ""}${if (state.isNotEmpty()) ", $state" else ""}"
        } ?: "Location TBD"
        
        val startDate = ticketmasterEvent.dates?.start?.let { start ->
            start.dateTime ?: "${start.localDate ?: ""} ${start.localTime ?: ""}".trim()
        } ?: "Date TBD"
        
        val description = buildString {
            attraction?.let { append("${it.name}") }
            classification?.let { 
                if (isNotEmpty()) append(" • ")
                append("${it.segment?.name ?: ""} ${it.genre?.name ?: ""}".trim())
            }
            priceRange?.let {
                if (isNotEmpty()) append(" • ")
                append("From ${it.min?.toInt() ?: "TBD"}${it.currency ?: "$"}")
            }
        }
        
        return UnifiedEvent(
            id = ticketmasterEvent.id,
            title = ticketmasterEvent.name,
            description = if (description.isNotEmpty()) description else null,
            type = classification?.segment?.name ?: "Event",
            startDate = startDate,
            location = location,
            url = ticketmasterEvent.url,
            source = "ticketmaster"
        )
    }

    private fun pushEventToApi(event: UnifiedEvent) {
        val userEventRequest = UserEventRequest(
            title = event.title,
            description = event.description,
            type = event.type,
            startDate = event.startDate,
            endDate = event.endDate,
            location = event.location,
            latitude = event.latitude,
            longitude = event.longitude
        )

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.eventiaApi.createEvent(userEventRequest)
                if (response.isSuccessful) {
                    // Replace the local event with the one returned from API
                    response.body()?.let { createdEvent ->
                        allEvents[0] = createdEvent
                        eventAdapter.updateEvents(allEvents)
                    }
                    Toast.makeText(requireContext(), "Event pushed to API!", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e("API", "Failed to push event: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("API", "Error pushing event", e)
            }
        }
    }


    // Permission flow handled via ActivityResultContracts above

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}