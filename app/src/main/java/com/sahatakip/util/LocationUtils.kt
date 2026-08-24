package com.sahatakip.util

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

object LocationUtils {
    /**
     * Calculates the distance between two points in meters using Android's built-in distanceBetween.
     */
    fun calculateDistanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0].toDouble()
    }

    suspend fun getAddressFromLocation(context: Context, lat: Double, lng: Double): String {
        if (!Geocoder.isPresent()) return "Bilinmeyen Konum ($lat, $lng)"

        return try {
            val geocoder = Geocoder(context, Locale.getDefault())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                suspendCancellableCoroutine { continuation ->
                    geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            val result = addresses.firstOrNull()?.let(::formatAddress)
                                ?: "Bilinmeyen Konum ($lat, $lng)"
                            continuation.resume(result)
                        }

                        override fun onError(errorMessage: String?) {
                            Timber.tag("LocationUtils").e("Geocode error: $errorMessage")
                            continuation.resume("Konum Çözülemedi ($lat, $lng)")
                        }
                    })
                }
            } else {
                withContext(Dispatchers.IO) {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(lat, lng, 1)
                    addresses?.firstOrNull()?.let(::formatAddress) ?: "Bilinmeyen Konum ($lat, $lng)"
                }
            }
        } catch (e: Exception) {
            Timber.tag("LocationUtils").e(e, "Geocoder exception: ${e.message}")
            "Konum Çözülemedi ($lat, $lng)"
        }
    }

    private fun formatAddress(address: Address): String =
        (0..address.maxAddressLineIndex).joinToString(", ", transform = address::getAddressLine)
}
