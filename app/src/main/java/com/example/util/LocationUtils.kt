package com.example.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import java.util.Locale
import kotlin.math.*

object LocationUtils {
    /**
     * Calculates the distance between two points in meters using Haversine formula.
     */
    fun calculateDistanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth's radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    @Suppress("DEPRECATION")
    fun getAddressFromLocation(context: Context, lat: Double, lng: Double): String {
        return try {
            val geocoder = Geocoder(context, Locale.getDefault())

            val addresses = geocoder.getFromLocation(lat, lng, 1)
            
            if (!addresses.isNullOrEmpty()) {
                formatAddress(addresses[0])
            } else {
                "Bilinmeyen Konum ($lat, $lng)"
            }
        } catch (e: Exception) {
            android.util.Log.e("LocationUtils", "Geocoder error: ${e.message}")
            "Konum Çözülemedi ($lat, $lng)"
        }
    }

    private fun formatAddress(address: Address): String {
        val addressParts = mutableListOf<String>()
        for (i in 0..address.maxAddressLineIndex) {
            addressParts.add(address.getAddressLine(i))
        }
        return addressParts.joinToString(", ")
    }
}
