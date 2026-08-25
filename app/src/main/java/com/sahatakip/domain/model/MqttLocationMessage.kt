package com.sahatakip.domain.model

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MqttLocationMessage(
    val deviceId: String,
    val latitude: Double,
    val longitude: Double,
    val speed: Float,
    val batteryLevel: Int,
    val timestamp: Long,
    val address: String? = null
)
