package com.locationtest.utils

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.locationtest.MainActivity
import com.locationtest.model.LocationData
import com.locationtest.repository.LocationRepository
import java.text.SimpleDateFormat
import java.util.*

class LocationService : Service() {

    companion object {
        private const val TAG = "LocationService"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "location_service_channel"
        private const val LOCATION_UPDATE_INTERVAL = 10000L // 10 seconds for better battery
        private const val FASTEST_LOCATION_INTERVAL = 5000L // 5 seconds
        private const val MAX_WAIT_TIME = 10000L // 1 minute
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var notificationManager: NotificationManager
    private var locationCallback: LocationCallback? = null
    private var currentLocationData: LocationData? = null
    private var isLocationUpdatesActive = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "LocationService onCreate")
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel()
        
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL)
            .setMinUpdateIntervalMillis(FASTEST_LOCATION_INTERVAL)
            .setWaitForAccurateLocation(false)
            .setMaxUpdateDelayMillis(MAX_WAIT_TIME)
            .build()

        // Start foreground immediately
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Start location updates if we have permission
        if (hasLocationPermission()) {
            startLocationUpdates()
            // Also get last known location immediately
            getLastKnownLocation()
        } else {
            Log.w(TAG, "Location permission not granted")
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || 
        ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getLastKnownLocation() {
        if (!hasLocationPermission()) return
        
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val data = LocationData(
                        latitude = it.latitude,
                        longitude = it.longitude,
                        timestamp = System.currentTimeMillis(),
                        accuracy = if (it.hasAccuracy()) it.accuracy else null,
                        altitude = if (it.hasAltitude()) it.altitude else null,
                        bearing = if (it.hasBearing()) it.bearing else null,
                        speed = if (it.hasSpeed()) it.speed else null,
                        provider = it.provider
                    )
                    
                    currentLocationData = data
                    updateNotification(data)
                    updateRepository(data)
                    Log.d(TAG, "Got last known location: ${it.latitude}, ${it.longitude}")
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Failed to get last known location", e)
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception getting last known location", e)
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Location Tracking Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Persistent location tracking in background"
            setShowBadge(false)
            setSound(null, null)
        }
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(locationData: LocationData? = null): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Location Tracker Active"
        val content = if (locationData != null) {
            val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val time = dateFormat.format(Date(locationData.timestamp))
            "Lat: ${String.format("%.6f", locationData.latitude)}\n" +
            "Lng: ${String.format("%.6f", locationData.longitude)}\n" +
            "Updated: $time"
        } else {
            "Getting location..."
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun updateNotification(locationData: LocationData) {
        val notification = createNotification(locationData)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun updateRepository(data: LocationData) {
        try {
            MainActivity.repository.updateLocation(data)
            Log.d(TAG, "Updated repository with location data")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update repository", e)
        }
    }

    private fun startLocationUpdates() {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Cannot start location updates - no permission")
            return
        }
        
        if (isLocationUpdatesActive) {
            Log.d(TAG, "Location updates already active")
            return
        }
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                Log.d(TAG, "Location update received: ${location.latitude}, ${location.longitude}")
                
                val data = LocationData(
                    latitude = location.latitude, 
                    longitude = location.longitude, 
                    timestamp = System.currentTimeMillis(),
                    accuracy = if (location.hasAccuracy()) location.accuracy else null,
                    altitude = if (location.hasAltitude()) location.altitude else null,
                    bearing = if (location.hasBearing()) location.bearing else null,
                    speed = if (location.hasSpeed()) location.speed else null,
                    provider = location.provider
                )
                
                currentLocationData = data
                updateNotification(data)
                updateRepository(data)
            }
        }

        locationCallback?.let { callback ->
            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
                isLocationUpdatesActive = true
                Log.d(TAG, "Location updates started successfully")
            } catch (e: SecurityException) {
                Log.e(TAG, "Security exception starting location updates", e)
                isLocationUpdatesActive = false
            } catch (e: Exception) {
                Log.e(TAG, "Exception starting location updates", e)
                isLocationUpdatesActive = false
            }
        }
    }

    private fun stopLocationUpdates() {
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
            isLocationUpdatesActive = false
            Log.d(TAG, "Location updates stopped")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand called")
        
        // Ensure we have foreground notification
        startForeground(NOTIFICATION_ID, createNotification(currentLocationData))
        
        // Restart location updates if they're not active and we have permission
        if (hasLocationPermission() && !isLocationUpdatesActive) {
            startLocationUpdates()
            getLastKnownLocation()
        }
        
        // Return START_STICKY to restart service if killed by system
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d(TAG, "onTaskRemoved called")
        super.onTaskRemoved(rootIntent)
        
        // Don't restart automatically when task is removed to save battery
        // The broadcast receiver will handle app restart if needed
    }

    override fun onDestroy() {
        Log.d(TAG, "LocationService onDestroy")
        super.onDestroy()
        
        stopLocationUpdates()
        
        // Try to restart the service if it's being destroyed unexpectedly
        if (hasLocationPermission()) {
            val restartServiceIntent = Intent(applicationContext, LocationService::class.java)
            try {
                startForegroundService(restartServiceIntent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restart service", e)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
