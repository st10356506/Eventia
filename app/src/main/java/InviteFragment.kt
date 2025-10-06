package com.example.eventplanner.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.eventplanner.R
import com.example.eventplanner.adapters.InviteAdapter
import com.example.eventplanner.models.Invite
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class InvitesFragment : Fragment() {

    private lateinit var invitesRef: DatabaseReference
    private lateinit var rvInvites: RecyclerView
    private lateinit var adapter: InviteAdapter
    private val inviteList = mutableListOf<Invite>()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_invite, container, false)

        rvInvites = view.findViewById(R.id.rv_invites)
        rvInvites.layoutManager = LinearLayoutManager(context)
        adapter = InviteAdapter(inviteList) { invite, newStatus ->
            val standardizedStatus =
                if (newStatus.equals("accepted", true)) "confirmed" else newStatus
            updateInviteStatus(invite, standardizedStatus)
        }
        rvInvites.adapter = adapter

        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Fetch username from Realtime Database
            val userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(currentUser.uid)

            userRef.child("username").get().addOnSuccessListener { snapshot ->
                val username = snapshot.getValue(String::class.java)
                if (username.isNullOrBlank()) {
                    Toast.makeText(requireContext(), "Username not set!", Toast.LENGTH_SHORT).show()
                } else {
                    // Reference invites by username
                    invitesRef = FirebaseDatabase.getInstance()
                        .getReference("invites")
                        .child(username)
                    loadInvitesRealtime()
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load username", Toast.LENGTH_SHORT)
                    .show()
                Log.e("InvitesFragment", "Failed to fetch username", it)
            }
        } else {
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
        }

        return view
    }

    /** Load invites sent to the current user's username in real-time **/
    private fun loadInvitesRealtime() {
        invitesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                inviteList.clear()
                snapshot.children.forEach { data ->
                    val invite = data.getValue(Invite::class.java)
                    invite?.let {
                        if (it.id.isNotBlank()) inviteList.add(it)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(
                    "InvitesFragment",
                    "Failed to load invites: ${error.message}",
                    error.toException()
                )
            }
        })
    }

    /** Update invite status (accept/decline) and also update sender's guest entry **/
    private fun updateInviteStatus(invite: Invite, newStatus: String) {
        invite.status = newStatus
        if (invite.id.isBlank()) return

        // Update recipient's invite
        invitesRef.child(invite.id).setValue(invite)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Invite $newStatus", Toast.LENGTH_SHORT).show()

                // Update sender's guest by inviteId
                val senderGuestRef = FirebaseDatabase.getInstance()
                    .getReference("guests")
                    .child(invite.senderUID)

                senderGuestRef.orderByChild("inviteId").equalTo(invite.id)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            snapshot.children.forEach { guestSnap ->
                                guestSnap.ref.child("status").setValue(newStatus)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e(
                                "InvitesFragment",
                                "Failed to update sender guest status",
                                error.toException()
                            )
                        }
                    })
            }
            .addOnFailureListener { e ->
                Log.e("InvitesFragment", "Failed to update invite", e)
                Toast.makeText(
                    requireContext(),
                    "Failed to update invite: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}