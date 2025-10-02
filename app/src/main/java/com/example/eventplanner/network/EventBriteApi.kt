package com.example.eventplanner.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface EventbriteApi {
    @GET("events/search/")
    suspend fun searchEvents(
        @Query("location.latitude") latitude: Double,
        @Query("location.longitude") longitude: Double,
        @Query("q") keyword: String? = null
    ): Response<EventResponse>
}

interface MyApi {
    @POST("events")
    suspend fun createUserEvent(@Body event: UserEventRequest): Response<Void>
}

