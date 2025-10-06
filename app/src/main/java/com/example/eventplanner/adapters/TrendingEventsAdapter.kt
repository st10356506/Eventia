package com.example.eventplanner.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.eventplanner.R
import com.example.eventplanner.models.Event

class TrendingEventsAdapter(private val events: List<Event>) :
    RecyclerView.Adapter<TrendingEventsAdapter.EventViewHolder>() {

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivEventImage: ImageView = itemView.findViewById(R.id.ivEventImage)
        val tvEventName: TextView = itemView.findViewById(R.id.tvEventName)
        val tvEventCategory: TextView = itemView.findViewById(R.id.tvEventCategory)
        val tvEventDate: TextView = itemView.findViewById(R.id.tvEventDate)
        val tvEventLocation: TextView = itemView.findViewById(R.id.tvEventLocation)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trending_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]
        holder.tvEventName.text = event.name
        holder.tvEventCategory.text = event.category
        holder.tvEventDate.text = event.date
        holder.tvEventLocation.text = event.location
        Glide.with(holder.itemView.context)
            .load(event.imageUrl)
            .centerCrop()
            .into(holder.ivEventImage)
    }

    override fun getItemCount(): Int = events.size
}
