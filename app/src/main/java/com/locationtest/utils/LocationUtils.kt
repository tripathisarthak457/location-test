package com.locationtest.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.locationtest.model.LocationData

object LocationUtils {
    private const val TAG = "LocationUtils"
    private const val TIMEOUT_MS = 30000L // 30 seconds timeout

    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || 
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || 
               locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun getCurrentLocation(context: Context, callback: (LocationData?) -> Unit) {
        if (!hasLocationPermission(context)) {
            Log.w(TAG, "No location permission")
            callback(null)
            return
        }
        
        if (!isLocationEnabled(context)) {
            Log.w(TAG, "Location services are disabled")
            callback(null)
            return
        }
        
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        
        // First try to get last known location
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                if (location != null && isLocationRecent(location)) {
                    Log.d(TAG, "Using recent last known location")
                    callback(createLocationData(location))
                } else {
                    Log.d(TAG, "Last known location is null or too old, requesting fresh location")
                    requestFreshLocation(context, fusedLocationClient, callback)
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to get last known location", e)
                requestFreshLocation(context, fusedLocationClient, callback)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting location", e)
            callback(null)
        }
    }
    
    private fun isLocationRecent(location: Location): Boolean {
        val locationAge = System.currentTimeMillis() - location.time
        return locationAge < 60000 // Consider location recent if less than 1 minute old
    }
    
    private fun requestFreshLocation(
        context: Context, 
        fusedLocationClient: FusedLocationProviderClient,
        callback: (LocationData?) -> Unit
    ) {
        if (!hasLocationPermission(context)) {
            callback(null)
            return
        }
        
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(1000L)
            .setWaitForAccurateLocation(false)
            .setMaxUpdateDelayMillis(10000L)
            .build()
        
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation
                if (location != null) {
                    Log.d(TAG, "Got fresh location: ${location.latitude}, ${location.longitude}")
                    callback(createLocationData(location))
                    
                    // Remove location updates after first result
                    try {
                        fusedLocationClient.removeLocationUpdates(this)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to remove location updates", e)
                    }
                } else {
                    Log.w(TAG, "Location result is null")
                }
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
            
            // Set timeout to avoid waiting forever
            android.os.Handler(Looper.getMainLooper()).postDelayed({
                try {
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                    Log.w(TAG, "Location request timed out")
                    callback(null)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to remove location updates on timeout", e)
                }
            }, TIMEOUT_MS)
            
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception requesting fresh location", e)
            callback(null)
        } catch (e: Exception) {
            Log.e(TAG, "Exception requesting fresh location", e)
            callback(null)
        }
    }
    
    private fun createLocationData(location: Location): LocationData {
        return LocationData(
            latitude = location.latitude,
            longitude = location.longitude,
            timestamp = System.currentTimeMillis(),
            accuracy = if (location.hasAccuracy()) location.accuracy else null,
            altitude = if (location.hasAltitude()) location.altitude else null,
            bearing = if (location.hasBearing()) location.bearing else null,
            speed = if (location.hasSpeed()) location.speed else null,
            provider = location.provider
        )
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
