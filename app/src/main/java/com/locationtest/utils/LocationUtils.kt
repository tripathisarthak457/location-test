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
                            timestamp = System.currentTimeMillis()
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
}
