package com.example.eventplanner.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.eventplanner.R
import com.example.eventplanner.databinding.ItemEventBinding
import com.example.eventplanner.network.UnifiedEvent
import java.text.SimpleDateFormat
import java.util.*

class EventAdapter(
    private var events: List<UnifiedEvent> = emptyList(),
    private val onEventClick: (UnifiedEvent) -> Unit = {}
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    class EventViewHolder(private val binding: ItemEventBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(event: UnifiedEvent, onEventClick: (UnifiedEvent) -> Unit) {
            binding.apply {
                tvEventTitle.text = event.title
                chipEventType.text = event.type
                tvEventDescription.text = event.description ?: "No description available"
                
                // Format date
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                tvEventDate.text = try {
                    val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                    val date = inputFormat.parse(event.startDate)
                    date?.let { dateFormat.format(it) } ?: event.startDate
                } catch (e: Exception) {
                    event.startDate
                }
                
                tvEventLocation.text = event.location ?: "Location not specified"
                
                // Hide budget for now since it's not in our model
                tvEventBudget.visibility = View.GONE
                
                root.setOnClickListener { onEventClick(event) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(events[position], onEventClick)
    }

    override fun getItemCount(): Int = events.size

    fun updateEvents(newEvents: List<UnifiedEvent>) {
        events = newEvents
        notifyDataSetChanged()
    }
}
