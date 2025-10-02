package com.example.eventplanner.network

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://www.eventbriteapi.com/v3/"
    private const val EVENTIA_BASE_URL = "https://localhost:5000/"

    val api: EventbriteApi by lazy {
        val okHttp = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer FV4VSB4Q6NVHU5Z22ZXF")
                    .build()
                chain.proceed(request)
            }.build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EventbriteApi::class.java)
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
}
