package com.sahatakip.util

import com.sahatakip.domain.model.MqttLocationMessage
import com.squareup.moshi.Moshi
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallback
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import timber.log.Timber
import java.util.UUID

object MqttHelper {
    private const val BROKER_URL = "tcp://test.mosquitto.org:1883"
    private val clientId = "SahaTakip_${UUID.randomUUID().toString().take(8)}"
    private var client: MqttClient? = null
    
    private val moshi = Moshi.Builder().build()
    private val adapter = moshi.adapter(MqttLocationMessage::class.java)

    fun connect() {
        if (client?.isConnected == true) return
        
        try {
            client = MqttClient(BROKER_URL, clientId, MemoryPersistence())
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 30
                keepAliveInterval = 60
                isAutomaticReconnect = true
            }
            
            client?.setCallback(object : MqttCallback {
                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    Timber.tag("MQTT").d("Message arrived on $topic: ${message?.toString()}")
                }
                override fun connectionLost(cause: Throwable?) {
                    Timber.tag("MQTT").w("Connection lost: ${cause?.message}")
                }
                override fun deliveryComplete(token: IMqttDeliveryToken?) {}
            })
            
            client?.connect(options)
            Timber.tag("MQTT").i("Connected to HiveMQ")
        } catch (e: Exception) {
            Timber.tag("MQTT").e(e, "Connection failed")
        }
    }

    fun publishLocation(topic: String, message: MqttLocationMessage) {
        try {
            if (client?.isConnected != true) {
                connect()
            }
            
            val json = adapter.toJson(message)
            val mqttMessage = MqttMessage(json.toByteArray()).apply {
                qos = 1
            }
            client?.publish(topic, mqttMessage)
            Timber.tag("MQTT").d("Published: $json")
        } catch (e: Exception) {
            Timber.tag("MQTT").e(e, "Publish failed")
        }
    }

    fun disconnect() {
        try {
            client?.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
