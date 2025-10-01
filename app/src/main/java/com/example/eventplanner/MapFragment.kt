package com.example.eventplanner

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.snackbar.Snackbar

class MapFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_map, container, false)
        setupClickListeners(view)
        return view
    }
    
    private fun setupClickListeners(view: View) {
        // Search button
        view.findViewById<View>(R.id.btn_search)?.setOnClickListener {
            Snackbar.make(
                view,
                "Search functionality coming soon!",
                Snackbar.LENGTH_SHORT
            ).show()
        }
        
        // Enable Location button
        view.findViewById<View>(R.id.btn_enable_location)?.setOnClickListener {
            Snackbar.make(
                view,
                "Location services will be enabled soon!",
                Snackbar.LENGTH_SHORT
            ).show()
        }
        
        // My Location FAB
        view.findViewById<View>(R.id.fab_my_location)?.setOnClickListener {
            Snackbar.make(
                view,
                "My Location functionality coming soon!",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }
}