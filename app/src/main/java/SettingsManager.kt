package com.example.eventplanner.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object SettingsManager {

    private const val PREFS_NAME = "eventia_settings"
    private const val KEY_THEME = "theme" // values: light, dark
    private const val KEY_LOCALE = "locale" // e.g., en, af, zu
    private const val KEY_PROFILE_PHOTO_URI = "profile_photo_uri"
    private const val KEY_DEFAULT_LATITUDE = "default_latitude"
    private const val KEY_DEFAULT_LONGITUDE = "default_longitude"
    private const val KEY_DEFAULT_LOCATION_NAME = "default_location_name"
    private const val KEY_EVENT_RADIUS = "event_radius" // in kilometers

    fun isDarkModeEnabled(context: Context): Boolean {
        val theme = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME, "light")
        return theme == "dark"
    }

    fun setDarkMode(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME, if (enabled) "dark" else "light")
            .apply()
        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun applySavedTheme(context: Context) {
        val isDark = isDarkModeEnabled(context)
        AppCompatDelegate.setDefaultNightMode(
            if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    fun saveLocale(context: Context, languageCode: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LOCALE, languageCode)
            .apply()
    }

    fun getSavedLocale(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LOCALE, null)
    }

    fun saveProfilePhotoUri(context: Context, uri: String?) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PROFILE_PHOTO_URI, uri)
            .apply()
    }

    fun getProfilePhotoUri(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PROFILE_PHOTO_URI, null)
    }

    // Default Location Management
    fun saveDefaultLocation(context: Context, latitude: Double, longitude: Double, locationName: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_DEFAULT_LATITUDE, latitude.toFloat())
            .putFloat(KEY_DEFAULT_LONGITUDE, longitude.toFloat())
            .putString(KEY_DEFAULT_LOCATION_NAME, locationName)
            .apply()
    }

    fun getDefaultLocation(context: Context): Triple<Double, Double, String>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lat = prefs.getFloat(KEY_DEFAULT_LATITUDE, 0f).toDouble()
        val lng = prefs.getFloat(KEY_DEFAULT_LONGITUDE, 0f).toDouble()
        val name = prefs.getString(KEY_DEFAULT_LOCATION_NAME, null)

        return if (lat != 0.0 && lng != 0.0 && name != null) {
            Triple(lat, lng, name)
        } else null
    }

    fun hasDefaultLocation(context: Context): Boolean {
        return getDefaultLocation(context) != null
    }

    // Event Radius Management
    fun saveEventRadius(context: Context, radiusKm: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_EVENT_RADIUS, radiusKm)
            .apply()
    }

    fun getEventRadius(context: Context): Int {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_EVENT_RADIUS, 25) // Default to 25km
    }

    fun getEventRadiusInMeters(context: Context): Int {
        return getEventRadius(context) * 1000
    }
}