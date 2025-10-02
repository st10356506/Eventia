package com.example.eventplanner

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.example.eventplanner.databinding.FragmentEventBinding
import com.example.eventplanner.network.RetrofitClient
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EventFragment : Fragment(R.layout.fragment_event) {

    private var _binding: FragmentEventBinding? = null
    private val binding get() = _binding!!

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentEventBinding.bind(view)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Fetch events as soon as fragment opens
        getUserLocationAndFetchEvents()

        // Optional: button for manual refresh
        binding.fabNewEvent.setOnClickListener {
            getUserLocationAndFetchEvents()
        }
    }

    private fun getUserLocationAndFetchEvents() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
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
                val response = RetrofitClient.api.searchEvents(lat, lng)
                if (response.isSuccessful) {
                    val events = response.body()?.events ?: emptyList()
                    withContext(Dispatchers.Main) {
                        if (events.isNotEmpty()) {
                            Snackbar.make(
                                binding.root,
                                "Found ${events.size} events nearby!",
                                Snackbar.LENGTH_LONG
                            ).show()
                            // TODO: Display in RecyclerView
                        } else {
                            Snackbar.make(
                                binding.root,
                                "No events found near you.",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Snackbar.make(
                            binding.root,
                            "Error: ${response.errorBody()?.string()}",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Snackbar.make(
                        binding.root,
                        "Exception: ${e.message}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
