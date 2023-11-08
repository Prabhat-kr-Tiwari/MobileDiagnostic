package com.example.mobilediagnostic

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager

class LinearAccelerationSensorHelper(private val context: Context) {
    fun isLinearAccelerationSensorAvailable(): Boolean {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        return linearAccelerationSensor != null
    }
}
