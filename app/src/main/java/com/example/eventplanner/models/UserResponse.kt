package com.example.eventplanner.models

data class UserResponse(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val age: Int?,
    val createdAt: String
)
