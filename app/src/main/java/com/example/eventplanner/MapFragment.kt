package com.example.eventplanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.eventplanner.databinding.DialogLocationInputBinding
import com.example.eventplanner.databinding.FragmentMapBinding
import com.example.eventplanner.network.RetrofitClient
import com.example.eventplanner.network.TicketmasterEvent
import com.example.eventplanner.network.UnifiedEvent
import com.example.eventplanner.utils.LocationUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MapFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var googleMap: GoogleMap
    private var eventMarkers = mutableMapOf<Marker, UnifiedEvent>()
    private var currentClassification: String? = null
    private var currentKeyword: String? = null
    private var currentLocation: LatLng? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) getCurrentLocationAndLoadEvents() else showLocationInputDialog()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupLocationClient()
        setupClickListeners()
        setupMap()
        showLocationInputDialog()
    }

    private fun setupLocationClient() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
    }

    private fun setupClickListeners() {
        binding.btnRefreshMap.setOnClickListener { getCurrentLocationAndLoadEvents() }
        binding.btnLocationMap.setOnClickListener { showLocationInputDialog() }
        binding.btnCloseDetails.setOnClickListener { hideEventDetails() }
        binding.btnViewTickets.setOnClickListener {
            val currentEvent = eventMarkers.values.find { binding.cardEventDetails.visibility == View.VISIBLE }
            currentEvent?.url?.let { url ->
                Toast.makeText(requireContext(), "Opening tickets: $url", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupMap() {
        childFragmentManager.findFragmentById(R.id.map_view)?.let {
            childFragmentManager.beginTransaction().remove(it).commitNow()
        }

        val mapFragment = SupportMapFragment.newInstance()
        childFragmentManager.beginTransaction().replace(R.id.map_view, mapFragment).commitNow()
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.setInfoWindowAdapter(CustomInfoWindowAdapter())
        googleMap.setOnMarkerClickListener(this)
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.isMyLocationEnabled = true
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun getCurrentLocationAndLoadEvents() {
        val fineGranted = ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val latLng = LatLng(location.latitude, location.longitude)
                currentLocation = latLng
                CoroutineScope(Dispatchers.Main).launch {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
                    loadEventsForLocation(latLng.latitude, latLng.longitude)
                }
            } else {
                Snackbar.make(binding.root, "Could not get location", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadEventsForLocation(lat: Double, lng: Double) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.ticketmasterApi.searchEvents(
                    apiKey = "D5nbt3rsOCggZWiebPysFS6oLaiseKDy",
                    latlong = "$lat,$lng",
                    radius = "50",
                    keyword = currentKeyword,
                    classification = currentClassification,
                    size = "50",
                    sort = "date,asc"
                )

                if (response.isSuccessful) {
                    val events = response.body()?._embedded?.events?.map { convertTicketmasterToUnified(it) } ?: emptyList()
                    withContext(Dispatchers.Main) {
                        displayEventsOnMap(events)
                        Snackbar.make(binding.root, "Found events on map!", Snackbar.LENGTH_LONG).show()
                    }
                } else {
                    val errorText = try { response.errorBody()?.string() } catch (_: Exception) { null }
                    withContext(Dispatchers.Main) {
                        Snackbar.make(binding.root, "Failed to load events: ${response.code()}${if (!errorText.isNullOrBlank()) ": $errorText" else ""}", Snackbar.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(binding.root, "Error loading events: ${e.message}", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadEventsForLocation(location: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resolved = try { LocationUtils.getLocationFromAddress(requireContext(), location) } catch (_: Exception) { null }

                if (resolved != null) {
                    val latLng = LatLng(resolved.latitude, resolved.longitude)
                    currentLocation = latLng
                    withContext(Dispatchers.Main) {
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 10f))
                        loadEventsForLocation(latLng.latitude, latLng.longitude)
                    }
                } else {
                    val response = RetrofitClient.ticketmasterApi.searchEvents(
                        apiKey = "D5nbt3rsOCggZWiebPysFS6oLaiseKDy",
                        latlong = null,
                        radius = "50",
                        keyword = currentKeyword ?: location,
                        classification = currentClassification,
                        size = "50",
                        sort = "date,asc"
                    )

                    if (response.isSuccessful) {
                        val events = response.body()?._embedded?.events?.map { convertTicketmasterToUnified(it) } ?: emptyList()
                        withContext(Dispatchers.Main) {
                            displayEventsOnMap(events)
                            Snackbar.make(binding.root, "Found ${events.size} events for $location!", Snackbar.LENGTH_LONG).show()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Snackbar.make(binding.root, "Failed to load events for $location", Snackbar.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(binding.root, "Error loading events for $location: ${e.message}", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun displayEventsOnMap(events: List<UnifiedEvent>) {
        eventMarkers.clear()
        googleMap.clear()

        events.forEach { event ->
            val lat = event.latitude
            val lng = event.longitude
            if (lat != null && lng != null) {
                val marker = googleMap.addMarker(MarkerOptions().position(LatLng(lat, lng)).title(event.title).snippet(event.type))
                marker?.let { eventMarkers[it] = event }
            }
        }

        if (currentLocation == null && events.isNotEmpty()) {
            events.firstOrNull()?.let { first ->
                val lat = first.latitude
                val lng = first.longitude
                if (lat != null && lng != null) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 10f))
                }
            }
        }
    }

    private fun showLocationInputDialog() {
        val dialogBinding = DialogLocationInputBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext()).setView(dialogBinding.root).create()

        dialogBinding.btnCancelLocation.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnUseCurrentLocation.setOnClickListener {
            dialog.dismiss()
            getCurrentLocationAndLoadEvents()
        }
        dialogBinding.btnConfirmLocation.setOnClickListener {
            val input = dialogBinding.etLocationInput.text.toString().trim()
            if (input.isNotEmpty()) {
                dialog.dismiss()
                loadEventsForLocation(input)
            } else Toast.makeText(requireContext(), "Please enter a location", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        eventMarkers[marker]?.let { showEventDetails(it) }
        return true
    }

    private fun showEventDetails(event: UnifiedEvent) {
        binding.tvMapEventTitle.text = event.title
        binding.chipMapEventType.text = event.type
        binding.tvMapEventDescription.text = event.description ?: "No description available"
        binding.tvMapEventDate.text = try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", java.util.Locale.getDefault())
            inputFormat.parse(event.startDate)?.let { outputFormat.format(it) } ?: event.startDate
        } catch (e: Exception) { event.startDate }
        binding.tvMapEventLocation.text = event.location ?: "Location TBD"
        binding.cardEventDetails.visibility = View.VISIBLE
    }

    private fun hideEventDetails() {
        binding.cardEventDetails.visibility = View.GONE
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

        val startDate = ticketmasterEvent.dates?.start?.dateTime
            ?: "${ticketmasterEvent.dates?.start?.localDate ?: ""} ${ticketmasterEvent.dates?.start?.localTime ?: ""}".trim()

        val description = buildString {
            attraction?.let { append(it.name) }
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
            source = "ticketmaster",
            latitude = venue?.location?.latitude?.toDoubleOrNull(),
            longitude = venue?.location?.longitude?.toDoubleOrNull()
        )
    }

    private inner class CustomInfoWindowAdapter : GoogleMap.InfoWindowAdapter {
        private val window = layoutInflater.inflate(R.layout.custom_info_window, null)
        override fun getInfoWindow(marker: Marker): View? = null
        override fun getInfoContents(marker: Marker): View {
            val event = eventMarkers[marker]
            window.findViewById<TextView>(R.id.tv_info_title).text = event?.title ?: marker.title
            window.findViewById<TextView>(R.id.tv_info_snippet).text = event?.description ?: marker.snippet
            return window
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
