package com.example.eventplanner.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val address: String?
)

object LocationUtils {
    
    fun hasLocationPermission(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    suspend fun getCurrentLocation(context: Context): LocationResult? {
        if (!hasLocationPermission(context)) {
            return null
        }
        
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        return suspendCancellableCoroutine { continuation ->
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val geocoder = Geocoder(context)
                    val addresses = try {
                        geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    } catch (e: Exception) {
                        null
                    }
                    
                    val address = addresses?.firstOrNull()?.getAddressLine(0)
                    
                    continuation.resume(LocationResult(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        address = address
                    ))
                } else {
                    continuation.resume(null)
                }
            }.addOnFailureListener {
                continuation.resume(null)
            }
        }
    }
    
    suspend fun getLocationFromAddress(context: Context, address: String): LocationResult? {
        val geocoder = Geocoder(context)
        return try {
            val addresses = geocoder.getFromLocationName(address, 1)
            addresses?.firstOrNull()?.let { addressObj ->
                LocationResult(
                    latitude = addressObj.latitude,
                    longitude = addressObj.longitude,
                    address = address
                )
            }
        } catch (e: Exception) {
            null
        }
    }
}
