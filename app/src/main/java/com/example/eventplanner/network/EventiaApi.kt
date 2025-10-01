package com.example.eventplanner.network

// network/EventiaApi.kt
import com.example.eventplanner.models.UserRequest
import com.example.eventplanner.models.UserResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.Response

interface EventiaApi {
    @POST("/api/users")
    suspend fun createUser(@Body user: UserRequest): Response<UserResponse>

    @GET("/api/users/{id}")
    suspend fun getUser(@Path("id") id: String): Response<UserResponse>

    // add update/delete etc. if needed
}