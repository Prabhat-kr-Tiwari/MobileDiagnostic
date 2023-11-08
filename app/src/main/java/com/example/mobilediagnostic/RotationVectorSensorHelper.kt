package com.example.mobilediagnostic

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager

class RotationVectorSensorHelper(private val context: Context) {
    fun isRotationVectorSensorWorking(): Boolean {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        return rotationVectorSensor != null
    }
}
