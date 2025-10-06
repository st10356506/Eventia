package com.example.eventplanner

import android.content.Intent
import android.content.res.Configuration
import android.location.Geocoder
import android.os.Build
import android.os.Bundle
import android.provider.Settings as AndroidSettings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.eventplanner.databinding.FragmentSettingsBinding
import com.example.eventplanner.utils.SettingsManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.eventplanner.utils.LocationUtils

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private var pendingLocaleCode: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupProfileHeader()
        setupThemeSwitch()
        setupLanguagePicker()
        setupDefaultLocation()
        setupEventRadius()
        setupUsernameChange()

        binding.btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(requireContext(), Login::class.java))
            requireActivity().finish()
        }

        binding.btnManageNotifications.setOnClickListener {
            val intent = when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(AndroidSettings.EXTRA_APP_PACKAGE, requireContext().packageName)
                    }
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
                    Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra("app_package", requireContext().packageName)
                        putExtra("app_uid", requireContext().applicationInfo.uid)
                    }
                }
                else -> Intent(
                    "android.settings.APPLICATION_DETAILS_SETTINGS",
                    android.net.Uri.parse("package:" + requireContext().packageName)
                )
            }
            startActivity(intent)
        }

        binding.btnPrivacyPolicy.setOnClickListener { showPrivacyPolicy() }

        binding.btnSaveChanges.setOnClickListener {
            // Persist theme
            val isDark = binding.switchTheme.isChecked
            SettingsManager.setDarkMode(requireContext(), isDark)

            // Persist and apply locale
            pendingLocaleCode?.let { code ->
                SettingsManager.saveLocale(requireContext(), code)
                applyLocale(code)
            }

            Snackbar.make(binding.root, R.string.changes_saved, Snackbar.LENGTH_SHORT).show()
            requireActivity().recreate()
        }
    }

    private fun setupLanguagePicker() {
        if (binding == null) {
            return
        }

        val languages = listOf(
            getString(R.string.lang_english) to "en",
            getString(R.string.lang_afrikaans) to "af",
            getString(R.string.lang_zulu) to "zu",
            getString(R.string.lang_xhosa) to "xh",
            getString(R.string.lang_spanish) to "es",
            getString(R.string.lang_german) to "de",
            getString(R.string.lang_french) to "fr",
            getString(R.string.lang_hindi) to "hi",
            getString(R.string.lang_mandarin) to "zh"
        )

        val entries = languages.map { it.first }
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, entries)
        (binding.spinnerLanguage as MaterialAutoCompleteTextView).setAdapter(adapter)

        val saved = SettingsManager.getSavedLocale(requireContext())
        val selectedIndex = languages.indexOfFirst { it.second == saved }
        if (selectedIndex >= 0) {
            binding.spinnerLanguage.setText(entries[selectedIndex], false)
            pendingLocaleCode = languages[selectedIndex].second
        }

        binding.spinnerLanguage.setOnItemClickListener { _, _, position, _ ->
            pendingLocaleCode = languages[position].second
        }

        // Ensure dropdown opens on focus/click
        binding.spinnerLanguage.setOnClickListener {
            (it as? MaterialAutoCompleteTextView)?.showDropDown()
        }
    }

    private fun applyLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun setupThemeSwitch() {
        if (binding == null) {
            return
        }

        val dark = SettingsManager.isDarkModeEnabled(requireContext())
        binding.switchTheme.isChecked = dark
        binding.switchTheme.setOnCheckedChangeListener { _, isChecked ->
            SettingsManager.setDarkMode(requireContext(), isChecked)
        }
    }

    private fun setupProfileHeader() {
        // Check if binding is available
        if (binding == null) {
            return
        }

        val authUser = FirebaseAuth.getInstance().currentUser
        binding.tvProfileEmail.text = authUser?.email ?: ""
        val uid = authUser?.uid
        if (uid != null) {
            val db = com.google.firebase.database.FirebaseDatabase.getInstance()
                .getReference("users").child(uid)
            db.get().addOnSuccessListener { snapshot ->
                // Check if binding is still available
                if (binding != null) {
                    val username = snapshot.child("username").getValue(String::class.java)
                    binding.tvProfileName.text = username ?: getString(R.string.user)
                }
            }.addOnFailureListener {
                // Check if binding is still available
                if (binding != null) {
                    binding.tvProfileName.text = getString(R.string.user)
                }
            }
        } else {
            binding.tvProfileName.text = getString(R.string.user)
        }

        SettingsManager.getProfilePhotoUri(requireContext())?.let { saved ->
            if (saved.isNotEmpty() && binding != null) {
                try { binding.imgProfile.setImageURI(android.net.Uri.parse(saved)) } catch (_: Exception) {}
            }
        }

        binding.btnEditProfile.setOnClickListener { pickProfilePhoto() }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null && binding != null) {
            try {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            SettingsManager.saveProfilePhotoUri(requireContext(), uri.toString())
            binding.imgProfile.setImageURI(uri)
            Snackbar.make(binding.root, R.string.profile_updated, Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun pickProfilePhoto() {
        pickImageLauncher.launch("image/*")
    }

    private fun showPrivacyPolicy() {
        val message = getString(R.string.privacy_policy_content)
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle(R.string.privacy_policy)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun setupDefaultLocation() {
        if (binding == null) {
            return
        }

        // Show current default location if set
        SettingsManager.getDefaultLocation(requireContext())?.let { (_, _, locationName) ->
            binding.tvCurrentLocation.text = getString(R.string.location_set, locationName)
            binding.tvCurrentLocation.visibility = View.VISIBLE
        }

        binding.btnSetDefaultLocation.setOnClickListener {
            showLocationPickerDialog()
        }
    }

    private fun showLocationPickerDialog() {
        val dialogView = layoutInflater.inflate(android.R.layout.simple_list_item_1, null)
        val editText = EditText(requireContext()).apply {
            hint = getString(R.string.search_location)
            setText(SettingsManager.getDefaultLocation(requireContext())?.third ?: "")
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.set_default_location)
            .setView(editText)
            .setPositiveButton(R.string.save) { _, _ ->
                val locationName = editText.text.toString().trim()
                if (locationName.isNotEmpty()) {
                    // Use geocoding to get actual coordinates for the location
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val locationResult = LocationUtils.getLocationFromAddress(requireContext(), locationName)
                            withContext(Dispatchers.Main) {
                                if (locationResult != null) {
                                    SettingsManager.saveDefaultLocation(
                                        requireContext(),
                                        locationResult.latitude,
                                        locationResult.longitude,
                                        locationName
                                    )
                                    if (binding != null) {
                                        binding.tvCurrentLocation.text = getString(R.string.location_set, locationName)
                                        binding.tvCurrentLocation.visibility = View.VISIBLE
                                        Snackbar.make(binding.root, getString(R.string.location_set, locationName), Snackbar.LENGTH_SHORT).show()
                                    }
                                } else {
                                    if (binding != null) {
                                        Snackbar.make(binding.root, "Location not found: $locationName", Snackbar.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                if (binding != null) {
                                    Snackbar.make(binding.root, "Error finding location: ${e.message}", Snackbar.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun setupEventRadius() {
        if (binding == null) {
            return
        }

        val radiusOptions = listOf(
            getString(R.string.radius_1km) to 1,
            getString(R.string.radius_5km) to 5,
            getString(R.string.radius_10km) to 10,
            getString(R.string.radius_25km) to 25,
            getString(R.string.radius_50km) to 50,
            getString(R.string.radius_100km) to 100
        )

        val entries = radiusOptions.map { it.first }
        val adapter = android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, entries)
        (binding.spinnerRadius as MaterialAutoCompleteTextView).setAdapter(adapter)

        val currentRadius = SettingsManager.getEventRadius(requireContext())
        val selectedIndex = radiusOptions.indexOfFirst { it.second == currentRadius }
        if (selectedIndex >= 0) {
            binding.spinnerRadius.setText(entries[selectedIndex], false)
        }

        binding.spinnerRadius.setOnItemClickListener { _, _, position, _ ->
            val selectedRadius = radiusOptions[position].second
            SettingsManager.saveEventRadius(requireContext(), selectedRadius)
            Snackbar.make(binding.root, "Radius set to ${selectedRadius}km", Snackbar.LENGTH_SHORT).show()
        }

        binding.spinnerRadius.setOnClickListener {
            (it as? MaterialAutoCompleteTextView)?.showDropDown()
        }
    }

    private fun setupUsernameChange() {
        if (binding == null) {
            return
        }

        binding.btnChangeUsername.setOnClickListener {
            showUsernameChangeDialog()
        }
    }

    private fun showUsernameChangeDialog() {
        val dialogView = layoutInflater.inflate(android.R.layout.simple_list_item_1, null)
        val editText = EditText(requireContext()).apply {
            hint = getString(R.string.enter_new_username)
            setText(binding.tvProfileName.text.toString())
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.change_username)
            .setView(editText)
            .setPositiveButton(R.string.save) { _, _ ->
                val newUsername = editText.text.toString().trim()
                if (validateUsername(newUsername)) {
                    updateUsernameInFirebase(newUsername)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun validateUsername(username: String): Boolean {
        return when {
            username.isEmpty() -> {
                Snackbar.make(binding.root, R.string.username_required, Snackbar.LENGTH_SHORT).show()
                false
            }
            username.length < 3 -> {
                Snackbar.make(binding.root, R.string.username_too_short, Snackbar.LENGTH_SHORT).show()
                false
            }
            username.length > 20 -> {
                Snackbar.make(binding.root, R.string.username_too_long, Snackbar.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun updateUsernameInFirebase(newUsername: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val database = FirebaseDatabase.getInstance()
            val userRef = database.getReference("users").child(user.uid)

            userRef.child("username").setValue(newUsername)
                .addOnSuccessListener {
                    if (binding != null) {
                        binding.tvProfileName.text = newUsername
                        Snackbar.make(binding.root, R.string.username_updated, Snackbar.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    if (binding != null) {
                        Snackbar.make(binding.root, R.string.username_error, Snackbar.LENGTH_SHORT).show()
                    }
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
