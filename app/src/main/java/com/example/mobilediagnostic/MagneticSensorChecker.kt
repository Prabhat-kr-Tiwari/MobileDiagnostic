package com.example.mobilediagnostic

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class MagneticSensorChecker(private val context: Context) : SensorEventListener {

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val magneticSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    init {
        magneticSensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            val magneticFieldX = event.values[0]
            val magneticFieldY = event.values[1]
            val magneticFieldZ = event.values[2]

            if (isMagneticFieldWorkingProperly(magneticFieldX, magneticFieldY, magneticFieldZ)) {
                // Magnetic sensor is working properly
                sensorManager.unregisterListener(this) // Stop listening
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this example
    }

    private fun isMagneticFieldWorkingProperly(x: Float, y: Float, z: Float): Boolean {
        // Implement your own criteria to check if the magnetic sensor data is within acceptable ranges.
        // Adjust the checks to match your specific requirements.

        // This is a basic example for demonstration purposes.
        return x != 0.0f || y != 0.0f || z != 0.0f
    }

    fun isMagneticSensorWorking(): Boolean {
        return magneticSensor != null
    }
}
