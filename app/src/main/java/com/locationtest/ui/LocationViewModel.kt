package com.locationtest.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locationtest.model.LocationData
import com.locationtest.repository.LocationRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LocationViewModel(private val repository: LocationRepository) : ViewModel() {
    val locationData: StateFlow<LocationData?> = repository.locationFlow

    fun updateLocation(data: LocationData) {
        viewModelScope.launch {
            repository.updateLocation(data)
        }
    }
}