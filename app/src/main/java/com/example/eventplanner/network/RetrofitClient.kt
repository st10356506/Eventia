package com.example.eventplanner.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.logging.HttpLoggingInterceptor

object RetrofitClient {
    private const val BASE_URL = "https://app.ticketmaster.com/discovery/v2/" // Ticketmaster Discovery API
    private const val EVENTIA_BASE_URL = "https://eventiarestapi.onrender.com/"

    val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val ticketmasterApi: TicketmasterApi by lazy {
        val okHttp = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TicketmasterApi::class.java)
    }


    val eventiaApi: EventiaApi by lazy {
        val okHttp = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    // Add any authentication headers for Eventia API here if needed
                    .build()
                chain.proceed(request)
            }.build()

        Retrofit.Builder()
            .baseUrl(EVENTIA_BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EventiaApi::class.java)
    }

    val okHttp = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder().build()
            chain.proceed(request)
        }.build()

}
