package com.example.mobilediagnostic

import android.hardware.Sensor
import android.hardware.SensorManager

import android.content.Context


class GameRotationSensorHelper(private val context: Context) {
    fun isGameRotationSensorWorking(): Boolean {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val gameRotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)

        return gameRotationSensor != null
    }
}
