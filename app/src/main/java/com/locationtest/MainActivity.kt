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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.locationtest.repository.LocationRepository
import com.locationtest.ui.LocationScreen
import com.locationtest.ui.LocationViewModel
import com.locationtest.ui.theme.LocationTestTheme
import com.locationtest.utils.LocationService
import com.locationtest.utils.LocationUtils

class MainActivity : ComponentActivity() {
    companion object {
        lateinit var repository: LocationRepository
            private set
    }
    
    private lateinit var viewModel: LocationViewModel

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        if (fineLocationGranted || coarseLocationGranted) {
            requestBatteryOptimizationExemption()
            startLocationService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize singleton repository
        repository = LocationRepository()
        viewModel = LocationViewModel(repository)

        LocationUtils.init(this)

        checkAndRequestPermissions()

        setContent {
            LocationTestTheme {
                LocationScreen(viewModel)
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val permissionsToRequest = mutableListOf<String>()

        if (fineLocationPermission != PackageManager.PERMISSION_GRANTED && 
            coarseLocationPermission != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.addAll(listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ))
        }

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notificationPermission = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            )
            if (notificationPermission != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            requestBatteryOptimizationExemption()
            startLocationService()
        }
    }

    private fun requestBatteryOptimizationExemption() {
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
    
    private fun startLocationService() {
        val intent = Intent(this, LocationService::class.java)
        startForegroundService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Don't stop the service when activity is destroyed - we want it to keep running
    }
}
