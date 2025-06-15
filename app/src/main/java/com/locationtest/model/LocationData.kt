package com.locationtest.model

import java.sql.Timestamp

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val id : String = "test1"
)