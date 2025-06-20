package com.locationtracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts

class PermissionHelper(private val activity: MainActivity) {
    var requestCount = 0
    private val locationPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true -> {
                activity.startLocationService()
            }
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                // Coarse location granted
                activity.startLocationService()
            }
            else -> {
                // Location permission denied
                activity.showToast("Location permission required")
            }
        }
    }

    private val notificationPermissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkLocationPermissions()
        } else {
            activity.showToast("Notifications will not be shown")
        }
    }

    fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED) {
                checkLocationPermissions()
            } else {
                if (requestCount <= 2){
                    requestCount ++
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            checkLocationPermissions()
        }
    }

    private fun checkLocationPermissions() {
        when {
            activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED -> {
                activity.startLocationService()
            }
            activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED -> {
                activity.startLocationService()
            }
            else -> {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }
}