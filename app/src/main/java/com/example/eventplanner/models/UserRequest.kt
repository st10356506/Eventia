package com.example.eventplanner.models

data class UserRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val age: Int? = null
)