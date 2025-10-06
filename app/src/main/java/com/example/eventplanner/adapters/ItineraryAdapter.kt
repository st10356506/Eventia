package com.example.eventplanner.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.eventplanner.databinding.ItemItineraryBinding
import com.example.eventplanner.models.ItineraryItem

class ItineraryAdapter(
    private val items: MutableList<ItineraryItem>,
    private val onDeleteClick: (ItineraryItem) -> Unit
) : RecyclerView.Adapter<ItineraryAdapter.ItineraryViewHolder>() {

    inner class ItineraryViewHolder(val binding: ItemItineraryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItineraryViewHolder {
        val binding = ItemItineraryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ItineraryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItineraryViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            tvItineraryTitle.text = item.title.ifEmpty { "Untitled Event" }
            tvItineraryDescription.text = item.description.ifEmpty { "No description" }
            tvItineraryLocation.text = item.location.ifEmpty { "No location" }
            tvItineraryDate.text = item.date.ifEmpty { "No date" }
            tvTime.text = item.time.ifEmpty { "No time" }
            tvItineraryDuration.text = item.duration.ifEmpty { "N/A" }

            // Delete button — make sure your item_itinerary.xml includes this id:
            // <MaterialButton android:id="@+id/btn_delete_itinerary" ... />
            btnDeleteItinerary.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun addItem(item: ItineraryItem) {
        val existingIndex = items.indexOfFirst { it.id == item.id }
        if (existingIndex == -1) {
            items.add(item)
            notifyItemInserted(items.size - 1)
        } else {
            items[existingIndex] = item
            notifyItemChanged(existingIndex)
        }
    }

    fun updateItems(newItems: List<ItineraryItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun updateItem(item: ItineraryItem) {
        val index = items.indexOfFirst { it.id == item.id }
        if (index != -1) {
            items[index] = item
            notifyItemChanged(index)
        }
    }

    fun removeItem(itemId: String) {
        val index = items.indexOfFirst { it.id == itemId }
        if (index != -1) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}