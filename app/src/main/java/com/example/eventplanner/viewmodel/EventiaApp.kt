package com.example.eventplanner

import android.app.Application
import android.content.res.Configuration
import com.example.eventplanner.utils.SettingsManager
import java.util.Locale

class EventiaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Apply saved theme and locale as early as possible
        SettingsManager.applySavedTheme(this)
        applySavedLocale()
    }

    private fun applySavedLocale() {
        val code = SettingsManager.getSavedLocale(this) ?: return
        val locale = Locale(code)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}

