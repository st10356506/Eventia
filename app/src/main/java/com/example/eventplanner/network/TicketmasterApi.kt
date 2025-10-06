package com.example.eventplanner.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface TicketmasterApi {
    // Using Ticketmaster Discovery API for real event data
    @GET("events.json")
    suspend fun searchEvents(
        @Query("apikey") apiKey: String = "D5nbt3rsOCggZWiebPysFS6oLaiseKDy",
        @Query("latlong") latlong: String? = null, // Format: "lat,long"
        @Query("radius") radius: String = "25", // miles
        @Query("keyword") keyword: String? = null,
        @Query("classificationName") classification: String? = null, // music, sports, arts, etc.
        @Query("size") size: String = "20", // number of results
        @Query("sort") sort: String = "date,asc" // sort by date
    ): Response<TicketmasterResponse>

    @GET("events.json")
    suspend fun getTrendingEvents(
        @Query("apikey") apiKey: String,
        @Query("size") size: Int = 10,
        @Query("sort") sort: String = "date,asc",
    ): Response<TicketmasterResponse>

}

