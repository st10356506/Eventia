package com.example.eventplanner.models

data class UserRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val age: Int? = null
)
data class UserEventRequest(
    val title: String,
    val description: String? = null,
    val type: String,
    val startDate: String,
    val endDate: String? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdBy: String? = null
)

// Event model (used across your app)
data class UnifiedEvent(
    val id: String? = null,
    val userId: String? = null,
    val title: String,
    val description: String? = null,
    val type: String,
    val startDate: String,
    val endDate: String? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val url: String? = null,
    val source: String = "user",
    val createdBy: String? = null
)