package com.example.eventplanner.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.eventplanner.R
import com.example.eventplanner.models.Invite
import com.google.android.material.button.MaterialButton

class InviteAdapter(
    private val invites: List<Invite>,
    private val onStatusChange: (Invite, String) -> Unit
) : RecyclerView.Adapter<InviteAdapter.InviteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InviteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_invite, parent, false)
        return InviteViewHolder(view)
    }

    override fun onBindViewHolder(holder: InviteViewHolder, position: Int) {
        val invite = invites[position]
        holder.bind(invite)
    }

    override fun getItemCount(): Int = invites.size

    inner class InviteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvName: TextView = itemView.findViewById(R.id.tv_inviter_name)
        private val tvStatus: TextView = itemView.findViewById(R.id.tv_invite_status)
        private val tvMessage: TextView = itemView.findViewById(R.id.tv_invite_message)
        private val btnAccept: MaterialButton = itemView.findViewById(R.id.btn_accept)
        private val btnDecline: MaterialButton = itemView.findViewById(R.id.btn_decline)

        fun bind(invite: Invite) {
            tvName.text = "${invite.firstName} ${invite.lastName}"
            tvStatus.text = invite.status.replaceFirstChar { it.uppercase() }
            tvMessage.text = "You are invited by ${invite.senderUID ?: invite.senderUID}"

            btnAccept.setOnClickListener { onStatusChange(invite, "accepted") }
            btnDecline.setOnClickListener { onStatusChange(invite, "declined") }

            // Disable buttons if already responded
            val responded = invite.status.lowercase() != "pending"
            btnAccept.isEnabled = !responded
            btnDecline.isEnabled = !responded
        }
    }
}