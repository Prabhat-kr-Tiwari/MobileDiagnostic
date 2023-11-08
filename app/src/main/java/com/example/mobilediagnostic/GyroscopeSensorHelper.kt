package com.example.mobilediagnostic

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager

class GyroscopeSensorHelper(private val context: Context) {
    fun isGyroscopeSensorWorking(): Boolean {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gyroscopeSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

        return gyroscopeSensor != null
    }
}
