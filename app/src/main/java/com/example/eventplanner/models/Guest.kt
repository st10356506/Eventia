package com.example.eventplanner.models

data class Guest(
    var id: String = "",
    var firstName: String = "",
    var lastName: String = "",
    var username: String = "",            // Replaced email → username
    var phone: String = "",
    var guestType: String = "",           // Guest category/type
    var plusOne: String = "",             // Optional
    var dietaryRestrictions: String = "", // Optional
    var status: String = "Pending" ,
    var inviteId: String = "",
)