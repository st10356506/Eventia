package com.example.eventplanner.models

data class Invite(
    var id: String = "",
    var firstName: String = "",
    var lastName: String = "",
    var phone: String = "",
    var guestType: String = "",
    var plusOne: String = "",
    var dietaryRestrictions: String = "",
    var status: String = "pending",
    var senderUID: String = ""
)
