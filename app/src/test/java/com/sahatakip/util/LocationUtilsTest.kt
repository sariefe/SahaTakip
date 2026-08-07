package com.sahatakip.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationUtilsTest {

    @Test
    fun `calculateDistanceInMeters returns correct distance for known coordinates`() {
        val lat1 = 41.0054
        val lon1 = 28.9768
        val lat2 = 39.9251
        val lon2 = 32.8369
        
        val distance = LocationUtils.calculateDistanceInMeters(lat1, lon1, lat2, lon2)
        
        // Distance is approx 347.9 km based on Haversine formula
        assertEquals(347900.0, distance, 100.0) 
    }

    @Test
    fun `calculateDistanceInMeters returns 0 for same point`() {
        val lat = 41.0054
        val lon = 28.9768
        val distance = LocationUtils.calculateDistanceInMeters(lat, lon, lat, lon)
        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun `calculateDistanceInMeters handles antipodal points`() {
        val distance = LocationUtils.calculateDistanceInMeters(90.0, 0.0, -90.0, 0.0)
        assertEquals(Math.PI * 6371000.0, distance, 100.0)
    }

    @Test
    fun `calculateDistanceInMeters handles very small distances`() {
        // Approx 1 meter difference in latitude
        val lat1 = 41.000000
        val lon = 28.000000
        val lat2 = 41.000009 // ~1 meter at this latitude
        val distance = LocationUtils.calculateDistanceInMeters(lat1, lon, lat2, lon)
        assertEquals(1.0, distance, 0.1)
    }
}
