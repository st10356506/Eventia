package com.example.eventplanner.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.eventplanner.MainActivity
import com.example.eventplanner.R
import com.example.eventplanner.adapters.GuestAdapter
import com.example.eventplanner.models.Guest
import com.example.eventplanner.models.Invite
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class GuestFragment : Fragment() {

    private lateinit var database: FirebaseDatabase
    private lateinit var guestsRef: DatabaseReference
    private lateinit var tvConfirmed: TextView
    private lateinit var tvPending: TextView
    private lateinit var tvDeclined: TextView
    private lateinit var rvGuests: RecyclerView
    private lateinit var adapter: GuestAdapter
    private val guestList = mutableListOf<Guest>()
    private lateinit var btnInvite: MaterialButton
    private lateinit var btnInvites: MaterialButton
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_guest, container, false)

        database = FirebaseDatabase.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            guestsRef = database.getReference("guests").child(currentUser.uid)
        } else {
            Toast.makeText(requireContext(), "User not logged in!", Toast.LENGTH_SHORT).show()
            return view
        }

        tvConfirmed = view.findViewById(R.id.tv_confirmed_count)
        tvPending = view.findViewById(R.id.tv_pending_count)
        tvDeclined = view.findViewById(R.id.tv_declined_count)

        rvGuests = view.findViewById(R.id.rv_guests)
        rvGuests.layoutManager = LinearLayoutManager(context)
        rvGuests.setHasFixedSize(true)
        adapter = GuestAdapter(guestList)
        rvGuests.adapter = adapter

        btnInvite = view.findViewById(R.id.btn_invite_guest)
        btnInvite.setOnClickListener {
            (activity as? MainActivity)?.showGuestDialog(object : MainActivity.GuestDialogListener {
                override fun onGuestCreated(guest: Guest) {
                    saveGuestToFirebase(guest)
                    sendInviteToRecipient(guest)
                }
            })
        }

        btnInvites = view.findViewById(R.id.btn_invites)
        btnInvites.setOnClickListener {
            findNavController().navigate(R.id.action_guestFragment_to_invitesFragment)
        }

        // Real-time listeners
        loadGuestsRealtime()
        listenToSentInvitesUpdates()

        return view
    }

    private fun saveGuestToFirebase(guest: Guest) {
        val newGuestRef = guestsRef.push()
        guest.id = newGuestRef.key ?: ""
        newGuestRef.setValue(guest)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Guest saved!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to save guest: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("GuestFragment", "Failed to save guest", e)
            }
    }

    private fun sendInviteToRecipient(guest: Guest) {
        if (guest.username.isBlank()) return

        val recipientRef = database.getReference("invites").child(guest.username)
        val newInviteRef = recipientRef.push()
        val inviteId = newInviteRef.key ?: ""

        guest.inviteId = inviteId // link guest to invite
        guestsRef.child(guest.id).setValue(guest)

        val invite = Invite(
            id = inviteId,
            firstName = guest.firstName,
            lastName = guest.lastName,
            phone = guest.phone,
            guestType = guest.guestType,
            plusOne = guest.plusOne,
            dietaryRestrictions = guest.dietaryRestrictions,
            status = "pending",
            senderUID = auth.currentUser?.uid ?: "unknown"
        )

        newInviteRef.setValue(invite)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Invite sent to ${guest.username}", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("GuestFragment", "Failed to send invite", e)
                Toast.makeText(requireContext(), "Failed to send invite: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /** Listen to guests in real-time **/
    private fun loadGuestsRealtime() {
        guestsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                guestList.clear()
                snapshot.children.forEach { data ->
                    val guest = data.getValue(Guest::class.java)
                    guest?.let { guestList.add(it) }
                }
                adapter.notifyDataSetChanged()
                updateStatusCards()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GuestFragment", "Failed to load guests: ${error.message}", error.toException())
            }
        })
    }

    /** NEW: Listen to all /invites/{username}/ where senderUID == currentUser **/
    private fun listenToSentInvitesUpdates() {
        val currentUid = auth.currentUser?.uid ?: return
        val invitesRoot = database.getReference("invites")

        // Go through each username node (recipient)
        invitesRoot.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                attachInviteStatusListener(snapshot.key ?: "", currentUid)
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    /** Attach listener to invites under specific recipient username **/
    private fun attachInviteStatusListener(recipientUsername: String, currentUid: String) {
        val userInvitesRef = database.getReference("invites").child(recipientUsername)
        userInvitesRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach { data ->
                    val invite = data.getValue(Invite::class.java)
                    if (invite != null && invite.senderUID == currentUid) {
                        // Update corresponding guest
                        guestList.find { it.inviteId == invite.id }?.status = invite.status
                    }
                }
                adapter.notifyDataSetChanged()
                updateStatusCards()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GuestFragment", "Failed to listen to invites for $recipientUsername: ${error.message}")
            }
        })
    }

    /** Update cards **/
    private fun updateStatusCards() {
        val confirmed = guestList.count { it.status.equals("confirmed", true) }
        val pending = guestList.count { it.status.equals("pending", true) }
        val declined = guestList.count { it.status.equals("declined", true) }

        tvConfirmed.text = confirmed.toString()
        tvPending.text = pending.toString()
        tvDeclined.text = declined.toString()
    }
}