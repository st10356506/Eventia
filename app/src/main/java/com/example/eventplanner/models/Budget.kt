package com.example.eventplanner.models

data class Budget(
    var id: String = "",
    var title: String = "",
    var description: String = "",
    var amount: Double = 0.0,
    var currency: String = "",
    var category: String = "",
    var date: String = "",
    var paymentMethod: String = "",
    var notes: String = ""
)