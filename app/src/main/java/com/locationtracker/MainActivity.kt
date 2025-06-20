package com.locationtracker

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.locationtracker.location.GeofenceHelper
import com.locationtracker.location.LocationForegroundService
import com.locationtracker.location.LocationWorker
import com.locationtracker.ui.theme.LocationTrackerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainActivityViewModel by viewModels()
    private lateinit var permissionHelper: PermissionHelper
    private fun hasLocationPermission(): Boolean {
        return checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionHelper = PermissionHelper(this)
        enableEdgeToEdge()
        setContent {
            LocationTrackerTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(modifier = Modifier.padding(innerPadding)) {
                        if (viewModel.permissionState){
                            LocationTrackingScreen(
                                viewModel = viewModel,
                                onStartTracking = {
                                    startLocationService()
                                },
                                onStopTracking = {
                                    LocationForegroundService.stopService(this@MainActivity)
                                    LocationWorker.cancelWork(this@MainActivity)
                                }
                            )
                        } else {
                            PermissionScreen()
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionHelper.checkAndRequestPermissions()
        viewModel.permissionState = hasLocationPermission()
    }

    fun startLocationService() {
        if (hasLocationPermission()){
            viewModel.permissionState = true
            LocationForegroundService.startService(this)
            LocationWorker.scheduleWork(this)
            val geofenceHelper = GeofenceHelper(this)
            geofenceHelper.addGeofence("My Home", 22.7504065, 75.8717411, 100f)
        }
    }

    fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }


    override fun onDestroy() {
        super.onDestroy()
        LocationForegroundService.stopService(this)
        LocationWorker.cancelWork(this)
    }

}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LocationTrackerTheme {
        Greeting("Android")
    }
}


@Composable
fun LocationTrackingScreen(
    viewModel: MainActivityViewModel,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "Location Tracker",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                // Location information
                LocationInfoSection(viewModel)

                // Tracking status
                TrackingStatusSection(viewModel.isTracking)

                // Action buttons
                ActionButtons(
                    isTracking = viewModel.isTracking,
                    onStartTracking = {
                        viewModel.isTracking = true
                        onStartTracking()
                        showSnackbar(
                            context = context,
                            scope = coroutineScope,
                            snackbarHostState = snackbarHostState,
                            message = "Location tracking started"
                        )
                    },
                    onStopTracking = {
                        viewModel.isTracking = false
                        onStopTracking()
                        showSnackbar(
                            context = context,
                            scope = coroutineScope,
                            snackbarHostState = snackbarHostState,
                            message = "Location tracking stopped"
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun LocationInfoSection(viewModel: MainActivityViewModel) {
    val location = viewModel.locationFlow.collectAsState().value

    Column(
        modifier = Modifier.padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Current Location",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (location != null) {
            Text(
                text = "Latitude: ${location.latitude}",
                fontSize = 16.sp
            )
            Text(
                text = "Longitude: ${location.longitude}",
                fontSize = 16.sp
            )
            Text(
                text = "Accuracy: ${location.accuracy}m",
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            Text(
                text = "No location data available",
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun TrackingStatusSection(isTracking: Boolean) {
    Column(
        modifier = Modifier.padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isTracking) {
            CircularProgressIndicator(
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Tracking in progress...",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 16.sp
            )
        } else {
            Text(
                text = "Tracking stopped",
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
private fun ActionButtons(
    isTracking: Boolean,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit
) {
    Column(
        modifier = Modifier.padding(top = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (!isTracking) {
            Button(
                onClick = onStartTracking,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Start Tracking")
            }
        } else {
            Button(
                onClick = onStopTracking,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Stop Tracking")
            }
        }
    }
}

private fun showSnackbar(
    context: Context,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    message: String
) {
    scope.launch {
        snackbarHostState.showSnackbar(message)
    }
}

