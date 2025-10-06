package com.example.eventplanner.utils

import android.content.Context
import android.location.Location
import com.example.eventplanner.models.Event

object EventLocationUtils {

    /**
     * Filter events based on the user's default location and radius setting
     */
    fun filterEventsByLocation(context: Context, events: List<Event>): List<Event> {
        val defaultLocation = SettingsManager.getDefaultLocation(context)
        val radiusKm = SettingsManager.getEventRadius(context)

        if (defaultLocation == null) {
            // If no default location is set, return all events
            return events
        }

        val (lat, lng, _) = defaultLocation
        val userLocation = Location("").apply {
            latitude = lat
            longitude = lng
        }

        return events.filter { event ->
            // For demo purposes, we'll assume events have location data
            // In a real app, you'd get the event's coordinates from the API
            val eventLocation = Location("").apply {
                // This would come from the event's actual location data
                latitude = -26.2041 // Default to Johannesburg for demo
                longitude = 28.0473
            }

            val distanceKm = userLocation.distanceTo(eventLocation) / 1000f
            distanceKm <= radiusKm
        }
    }

    /**
     * Get the current search radius in kilometers
     */
    fun getSearchRadiusKm(context: Context): Int {
        return SettingsManager.getEventRadius(context)
    }

    /**
     * Get the default location name if set
     */
    fun getDefaultLocationName(context: Context): String? {
        return SettingsManager.getDefaultLocation(context)?.third
    }

    /**
     * Check if user has set a default location
     */
    fun hasDefaultLocation(context: Context): Boolean {
        return SettingsManager.hasDefaultLocation(context)
    }
}