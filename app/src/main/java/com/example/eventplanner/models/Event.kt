package com.example.eventplanner.models

data class Event(
    val id: String,
    val name: String,
    val imageUrl: String,
    val category: String,
    val date: String,
    val location: String
)