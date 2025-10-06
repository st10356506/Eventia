package com.example.eventplanner.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.eventplanner.R
import com.example.eventplanner.models.Guest

class GuestAdapter(private val guestList: List<Guest>) :
    RecyclerView.Adapter<GuestAdapter.GuestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_guest, parent, false)
        return GuestViewHolder(view)
    }

    override fun onBindViewHolder(holder: GuestViewHolder, position: Int) {
        val guest = guestList[position]
        holder.tvName.text = "${guest.firstName} ${guest.lastName}"

        holder.tvStatus.text = guest.status.replaceFirstChar { it.uppercase() }
    }

    override fun getItemCount(): Int = guestList.size

    class GuestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_guest_name)

        val tvStatus: TextView = itemView.findViewById(R.id.tv_guest_status)
    }
}