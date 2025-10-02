package com.example.eventplanner.models

data class User(
    val username: String,
    val name: String,
    val surname: String,
    val email: String,
    val age: Int = 0,
    val uid: String = ""
)