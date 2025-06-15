package com.locationtest

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.locationtest.repository.LocationRepository
import com.locationtest.ui.LocationScreen
import com.locationtest.ui.LocationViewModel
import com.locationtest.ui.theme.LocationTestTheme
import com.locationtest.utils.LocationService

class MainActivity : ComponentActivity() {
    companion object {
        lateinit var repository: LocationRepository
            private set
    }
    
    private lateinit var viewModel: LocationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize singleton repository
        repository = LocationRepository()
        viewModel = LocationViewModel(repository)

        setContent {
            LocationTestTheme {
                LocationScreen(viewModel)
            }
        }
    }

    fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    // Handle if battery optimization settings are not available
                }
            }
        }
    }
    
    fun startLocationService() {
        val intent = Intent(this, LocationService::class.java)
        startForegroundService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}
