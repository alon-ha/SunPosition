package com.alon.sunposition
import android.hardware.Sensor
import android.hardware.Sensor.TYPE_ACCELEROMETER
import android.hardware.Sensor.TYPE_MAGNETIC_FIELD
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.lang.Math.toDegrees
import android.content.Context
import io.reactivex.*
import io.reactivex.subjects.BehaviorSubject


interface CompassViewModeling: CompassViewModelingOutputs {
    val outputs: CompassViewModelingOutputs
    val inputs: CompassViewModelingInputs

}

interface CompassViewModelingInputs {

}

interface CompassViewModelingOutputs {
    val compassData: Observable<CompassData>
}


class CompassViewModel: CompassViewModeling, CompassViewModelingInputs, CompassViewModelingOutputs, SensorEventListener {

    override val inputs = this
    override val outputs = this

    private val context: Context
    private var sensorManager: SensorManager
    private val accelerometer: Sensor
    private val magnetometer: Sensor

    private var currentDegree = 0.0f
    private var lastAccelerometer = FloatArray(3)
    private var lastMagnetometer = FloatArray(3)
    private var lastAccelerometerSet = false
    private var lastMagnetometerSet = false

    private val compassDataInternal = BehaviorSubject.create<CompassData>()
    override val compassData = compassDataInternal.hide()

    constructor(context: Context) {
        this.context = context

        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(TYPE_MAGNETIC_FIELD)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor === accelerometer) {
            lowPass(event.values, lastAccelerometer)
            lastAccelerometerSet = true
        } else if (event.sensor === magnetometer) {
            lowPass(event.values, lastMagnetometer)
            lastMagnetometerSet = true
        }

        if (lastAccelerometerSet && lastMagnetometerSet) {
            val r = FloatArray(9)
            if (SensorManager.getRotationMatrix(r, null, lastAccelerometer, lastMagnetometer)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(r, orientation)
                val degree = (toDegrees(orientation[0].toDouble()) + 360).toFloat() % 360
                val newDegree = -degree

                currentDegree = newDegree
            }
        }
    }

    private fun lowPass(input: FloatArray, output: FloatArray) {
        val alpha = 0.05f

        for (i in input.indices) {
            output[i] = output[i] + alpha * (input[i] - output[i])
        }
    }
}