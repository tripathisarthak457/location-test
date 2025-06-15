package com.locationtest.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.locationtest.model.LocationData
import com.locationtest.repository.LocationRepository
import com.locationtest.utils.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class LocationUiState(
    val locationData: LocationData? = null,
    val isNetworkConnected: Boolean = true,
    val connectionType: String = "Unknown",
    val isLoading: Boolean = true,
    val error: String? = null
)

class LocationViewModel(
    private val repository: LocationRepository
) : ViewModel() {
    
    private lateinit var networkMonitor: NetworkMonitor
    
    private val _uiState = MutableStateFlow(LocationUiState())
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()
    
    val locationData: StateFlow<LocationData?> = repository.locationFlow

    fun initializeNetworkMonitor(context: Context) {
        networkMonitor = NetworkMonitor(context)
        
        viewModelScope.launch {
            combine(
                repository.locationFlow,
                networkMonitor.isConnected
            ) { location, isConnected ->
                _uiState.value = _uiState.value.copy(
                    locationData = location,
                    isNetworkConnected = isConnected,
                    connectionType = if (isConnected) networkMonitor.getConnectionType() else "No Connection",
                    isLoading = location == null && isConnected,
                    error = if (!isConnected) "No internet connection" else null
                )
            }.collect { }
        }
    }

    fun updateLocation(data: LocationData) {
        viewModelScope.launch {
            repository.updateLocation(data)
        }
    }
    
    fun retryConnection() {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null
        )
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
