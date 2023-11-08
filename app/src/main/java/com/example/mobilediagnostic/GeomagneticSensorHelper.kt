package com.example.mobilediagnostic

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager

class GeomagneticSensorHelper(private val context: Context) {
    fun isGeomagneticSensorAvailable(): Boolean {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val geomagneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        return geomagneticSensor != null
    }
}
