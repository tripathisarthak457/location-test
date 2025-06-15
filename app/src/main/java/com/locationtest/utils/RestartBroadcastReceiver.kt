package com.locationtest.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

class RestartBroadcastReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "RestartBroadcastReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        Log.d(TAG, "Received broadcast: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON" -> {
                
                // Only start service if we have location permission
                if (LocationUtils.hasLocationPermission(context)) {
                    Log.d(TAG, "Starting LocationService after system event")
                    
                    val serviceIntent = Intent(context, LocationService::class.java)
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            ContextCompat.startForegroundService(context, serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to start LocationService", e)
                    }
                } else {
                    Log.w(TAG, "Location permission not granted, not starting LocationService")
                }
            }
        }
    }
}
