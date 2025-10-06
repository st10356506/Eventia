package com.example.eventplanner.models

data class ItineraryItem(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var location: String = "",
    var date: String = "",    // Added date
    var time: String = "",    // Already existed
    var duration: String = "", // Added duration
    var type: String = ""     // e.g., Accommodation, Transport, etc.
)