package com.example.eventplanner.network

import com.example.eventplanner.models.UserRequest
import com.example.eventplanner.models.UserResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query


interface EventiaApi {
    @POST("users") suspend fun createUser(@Body user: UserRequest): Response<UserResponse>
    @GET("users/{id}") suspend fun getUser(@Path("id") id: String): Response<UserResponse>
    // Event management endpoints (using JSONPlaceholder free API as backend mock)
    // Maps to /posts for listing/creating items

    @GET("events")
    suspend fun getAllEvents(): Response<List<UnifiedEvent>>

    @GET("events")
    suspend fun getEventPosts(): Response<List<JsonPlaceholderPost>>

    @GET("events/{id}")
    suspend fun getEventById(@Path("id") id: String): Response<JsonPlaceholderPost>

    @POST("events")
    suspend fun createEvent(@Body event: UserEventRequest): Response<UnifiedEvent>
}

// network/EventiaApi.kt



// Unified Event Model for both APIs
data class UnifiedEvent(
    val id: String? = null,
    val title: String,
    val description: String? = null,
    val type: String,
    val startDate: String,
    val endDate: String? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val url: String? = null,
    val source: String = "user" // "user" or "ticketmaster"
)

// User Event Creation Request
data class UserEventRequest(
    val title: String,
    val description: String? = null,
    val type: String,
    val startDate: String,
    val endDate: String? = null,
    val location: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null
)

// Eventbrite API Models
data class EventResponse(
    val events: List<EventbriteEvent>
)

data class EventbriteEvent(
    val id: String,
    val name: Name,
    val description: Description? = null,
    val start: Start,
    val url: String,
    val venue: Venue? = null
)

data class Name(val text: String)
data class Description(val text: String)
data class Start(val local: String)
data class Venue(val name: String, val address: Address? = null)
data class Address(val city: String? = null, val region: String? = null, val country: String? = null)

// Ticketmaster Discovery API Models
data class TicketmasterResponse(
    val _embedded: TicketmasterEmbedded? = null,
    val page: TicketmasterPage? = null
)

data class TicketmasterEmbedded(
    val events: List<TicketmasterEvent>? = null
)

data class TicketmasterPage(
    val size: Int? = null,
    val totalElements: Int? = null,
    val totalPages: Int? = null,
    val number: Int? = null
)

data class TicketmasterEvent(
    val id: String,
    val name: String,
    val url: String? = null,
    val images: List<TicketmasterImage>? = null,
    val dates: TicketmasterDates? = null,
    val _embedded: TicketmasterEventEmbedded? = null,
    val priceRanges: List<TicketmasterPriceRange>? = null,
    val classifications: List<TicketmasterClassification>? = null
)

data class TicketmasterImage(
    val url: String,
    val width: Int? = null,
    val height: Int? = null,
    val fallback: Boolean? = null
)

data class TicketmasterDates(
    val start: TicketmasterStart? = null
)

data class TicketmasterStart(
    val localDate: String? = null,
    val localTime: String? = null,
    val dateTime: String? = null
)

data class TicketmasterEventEmbedded(
    val venues: List<TicketmasterVenue>? = null,
    val attractions: List<TicketmasterAttraction>? = null
)

data class TicketmasterVenue(
    val id: String,
    val name: String,
    val url: String? = null,
    val city: TicketmasterCity? = null,
    val state: TicketmasterState? = null,
    val country: TicketmasterCountry? = null,
    val address: TicketmasterAddress? = null,
    val location: TicketmasterVenueLocation? = null
)

data class TicketmasterAttraction(
    val id: String,
    val name: String,
    val url: String? = null
)

data class TicketmasterCity(
    val name: String
)

data class TicketmasterState(
    val name: String,
    val stateCode: String? = null
)

data class TicketmasterCountry(
    val name: String,
    val countryCode: String? = null
)

data class TicketmasterAddress(
    val line1: String? = null,
    val line2: String? = null
)

data class TicketmasterVenueLocation(
    val longitude: String? = null,
    val latitude: String? = null
)

data class TicketmasterPriceRange(
    val type: String? = null,
    val currency: String? = null,
    val min: Double? = null,
    val max: Double? = null
)

data class TicketmasterClassification(
    val primary: Boolean? = null,
    val segment: TicketmasterSegment? = null,
    val genre: TicketmasterGenre? = null,
    val subGenre: TicketmasterSubGenre? = null
)

data class TicketmasterSegment(
    val name: String
)

data class TicketmasterGenre(
    val name: String
)

data class TicketmasterSubGenre(
    val name: String
)

// JSONPlaceholder DTOs used to mock user events
data class JsonPlaceholderPost(
    val userId: Int? = null,
    val id: Int? = null,
    val title: String,
    val body: String
)

data class JsonPlaceholderCreatePost(
    val title: String,
    val body: String,
    val userId: Int = 1
)