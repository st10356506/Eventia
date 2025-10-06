package com.example.eventplanner

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.eventplanner.adapters.TrendingEventsAdapter
import com.example.eventplanner.models.Event
import com.example.eventplanner.network.RetrofitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardFragment : Fragment() {

    private lateinit var rvTrendingEvents: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var tvWelcome: TextView

    private val trendingEvents = mutableListOf<Event>()
    private lateinit var adapter: TrendingEventsAdapter

    // Replace with actual username retrieval
    private val username = "User"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        rvTrendingEvents = view.findViewById(R.id.rv_trending_events)
        etSearch = view.findViewById(R.id.et_search)
        tvWelcome = view.findViewById(R.id.tv_welcome)

        tvWelcome.text = "Welcome back, $username"

        adapter = TrendingEventsAdapter(trendingEvents)
        rvTrendingEvents.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rvTrendingEvents.adapter = adapter

        fetchTrendingEvents()

        // Search on keyboard enter
        etSearch.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN) {
                val query = etSearch.text.toString().trim()
                searchEvents(query)
                true
            } else false
        }

        return view
    }

    private fun fetchTrendingEvents() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.ticketmasterApi.getTrendingEvents(
                    apiKey = "D5nbt3rsOCggZWiebPysFS6oLaiseKDy",
                    size = 10
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    val events = body?._embedded?.events?.map { e ->
                        Event(
                            id = e.id,
                            name = e.name,
                            imageUrl = e.images?.firstOrNull()?.url ?: "",
                            category = e.classifications?.firstOrNull()?.segment?.name ?: "Other",
                            date = e.dates?.start?.localDate ?: "TBD",
                            location = e._embedded?.venues?.firstOrNull()?.city?.name ?: "TBD"
                        )
                    } ?: emptyList()

                    withContext(Dispatchers.Main) {
                        trendingEvents.clear()
                        trendingEvents.addAll(events)
                        adapter.notifyDataSetChanged()
                    }
                } else {
                    println("API error: ${response.code()}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    private fun searchEvents(query: String) {
        if (query.isBlank()) {
            fetchTrendingEvents()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.ticketmasterApi.searchEvents(
                    apiKey = "YOUR_TICKETMASTER_API_KEY",
                    keyword = query,
                    size = 10.toString()
                )

                val body = response.body()
                val events = body?._embedded?.events?.map { e ->
                    Event(
                        id = e.id,
                        name = e.name,
                        imageUrl = e.images?.firstOrNull()?.url ?: "",
                        category = e.classifications?.firstOrNull()?.segment?.name ?: "Other",
                        date = e.dates?.start?.localDate ?: "TBD",
                        location = e._embedded?.venues?.firstOrNull()?.city?.name ?: "TBD"
                    )
                } ?: emptyList()

                withContext(Dispatchers.Main) {
                    trendingEvents.clear()
                    trendingEvents.addAll(events)
                    adapter.notifyDataSetChanged()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
