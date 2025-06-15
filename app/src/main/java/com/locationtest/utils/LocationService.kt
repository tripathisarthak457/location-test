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
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "location_service_channel"
        private const val LOCATION_UPDATE_INTERVAL = 5000L // 5 seconds
        private const val FASTEST_LOCATION_INTERVAL = 5000L // 5 seconds
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var notificationManager: NotificationManager
    private var locationCallback: LocationCallback? = null
    private var currentLocationData: LocationData? = null

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel()
        
        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL)
            .setMinUpdateIntervalMillis(FASTEST_LOCATION_INTERVAL)
            .setWaitForAccurateLocation(false)
            .build()

        startForeground(NOTIFICATION_ID, createNotification())
        
        if (hasLocationPermission()) {
            startLocationUpdates()
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

    private fun startLocationUpdates() {
        if (!hasLocationPermission()) return
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
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
                
                // Update notification with new location
                updateNotification(data)
                
                // Update repository if available
                try {
                    MainActivity.repository.updateLocation(data)
                } catch (e: Exception) {
                    // Repository not initialized yet - that's ok, we still track location
                }
            }
        }

        locationCallback?.let { callback ->
            if (hasLocationPermission()) {
                try {
                    fusedLocationClient.requestLocationUpdates(locationRequest, callback, Looper.getMainLooper())
                } catch (e: SecurityException) {
                    // Handle permission error
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Restart location updates if they were stopped
        if (hasLocationPermission() && locationCallback == null) {
            startLocationUpdates()
        }
        
        // Return START_STICKY to restart service if killed by system
        return START_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // App was removed from recent tasks - restart the service
        val restartServiceIntent = Intent(applicationContext, LocationService::class.java)
        startForegroundService(restartServiceIntent)
        
        // Also send broadcast to restart if needed
        val restartIntent = Intent(applicationContext, RestartBroadcastReceiver::class.java)
        sendBroadcast(restartIntent)
        
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        locationCallback?.let { callback ->
            fusedLocationClient.removeLocationUpdates(callback)
        }
        
        // Try to restart the service
        val restartServiceIntent = Intent(applicationContext, LocationService::class.java)
        startForegroundService(restartServiceIntent)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
