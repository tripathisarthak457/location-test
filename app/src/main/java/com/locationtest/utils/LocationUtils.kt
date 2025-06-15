package com.locationtest.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.locationtest.model.LocationData

object LocationUtils {

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || 
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getCurrentLocation(context: Context, callback: (LocationData?) -> Unit) {
        if (!hasLocationPermission(context)) {
            callback(null)
            return
        }
        
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                location?.let {
                    callback(
                        LocationData(
                            latitude = it.latitude,
                            longitude = it.longitude,
                            timestamp = System.currentTimeMillis(),
                            accuracy = if (it.hasAccuracy()) it.accuracy else null,
                            altitude = if (it.hasAltitude()) it.altitude else null,
                            bearing = if (it.hasBearing()) it.bearing else null,
                            speed = if (it.hasSpeed()) it.speed else null,
                            provider = it.provider
                        )
                    )
                } ?: callback(null)
            }.addOnFailureListener {
                callback(null)
            }
        } catch (e: SecurityException) {
            callback(null)
        }
    }

    fun getAccuracyDescription(accuracy: Float?): String {
        return when {
            accuracy == null -> "Unknown"
            accuracy <= 5 -> "Excellent (±${accuracy.toInt()}m)"
            accuracy <= 10 -> "Good (±${accuracy.toInt()}m)"
            accuracy <= 50 -> "Fair (±${accuracy.toInt()}m)"
            else -> "Poor (±${accuracy.toInt()}m)"
        }
    }

    fun getSpeedInKmh(speedMs: Float?): String {
        return if (speedMs != null && speedMs > 0) {
            String.format("%.1f km/h", speedMs * 3.6)
        } else {
            "0 km/h"
        }
    }
}
