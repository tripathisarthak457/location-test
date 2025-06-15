package com.locationtest.repository

import com.locationtest.model.LocationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LocationRepository {
    private val _locationFlow = MutableStateFlow<LocationData?>(null)
    val locationFlow: StateFlow<LocationData?> = _locationFlow

    fun updateLocation(data: LocationData) {
        _locationFlow.value = data
    }
}