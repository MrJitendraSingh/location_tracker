package com.locationtracker

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.locationtracker.location.LocationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class MainActivityViewModel(application: Application) : AndroidViewModel(application) {

    var permissionState by mutableStateOf(false)
    var isTracking by mutableStateOf(false)
    private val locationRepository = LocationRepository.getInstance(application)

    val locationFlow = locationRepository.locationFlow
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )
}