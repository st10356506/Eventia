package com.example.eventplanner.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import com.example.eventplanner.models.UserRequest
import com.example.eventplanner.models.UserResponse

interface EventiaApi {
    @POST("users")
    suspend fun createUser(@Body user: UserRequest): Response<UserResponse>
    
    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: String): Response<UserResponse>
}

// network/EventiaApi.kt

data class EventResponse(
    val events: List<Event>
)

data class Event(
    val name: Name,
    val start: Start,
    val url: String
)

data class UserEventRequest(
    val title: String,
    val type: String,
    val startDate: String,
    val endDate: String
)

data class Name(val text: String)
data class Start(val local: String)