package com.alon.sunposition

import android.content.Context
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.withLatestFrom
import java.util.*
import kotlin.math.absoluteValue


interface MainActivityViewModeling {
    val outputs: MainActivityViewModelingOutputs
    val inputs: MainActivityViewModelingInputs

}

interface MainActivityViewModelingInputs {
    fun onResume()
    fun onPause()
    fun loadData()
    fun loadDataIfNeeded()
}

interface MainActivityViewModelingOutputs {
    val compassViewModel: CompassViewModeling
    val sunViewModel: SunViewModeling
    val sunPositionScreenRelative: Observable<SunAnimationData>
    val descriptionVisibility: Observable<Boolean>
    val descriptionTxt: Observable<String>
}


class MainActivityViewModel(context: Context): MainActivityViewModeling,
    MainActivityViewModelingInputs, MainActivityViewModelingOutputs {

    override val inputs = this
    override val outputs = this

    override val compassViewModel: CompassViewModeling = CompassViewModel(context)
    override val sunViewModel: SunViewModeling = SunViewModel(context)
    private val persistence: DataPersistencing = DataPersistence(context)

    private val thresholdToLoadDataAgain = 1000 * 60 * 10 // Every 10 minutes

    private val compassDegree: Observable<Float> = compassViewModel.outputs
        .compassPosition
        .map { pos -> pos.currentDegree }

    private val sunAzimuth: Observable<Double> = sunViewModel
        .outputs.sunPosition
        .map { pos -> pos.azimuth }

    override val sunPositionScreenRelative : Observable<SunAnimationData> = compassDegree
        .withLatestFrom(sunAzimuth)
        .map { (compassDegree, sunAzimuth) ->
            var relativeAzimuth = (sunAzimuth + compassDegree.toDouble()) % 360
            if (relativeAzimuth < 0) {
                relativeAzimuth += 360
            }
            relativeAzimuth
        }
        .map { degree -> degree.toFloat() }
        .scan (SunAnimationData(0f,0f, 0, 0), { animationData: SunAnimationData, degree: Float ->
            val radiusAddition = radiusAdditionFromDegree(degree)
            SunAnimationData(animationData.currentDegree, degree, animationData.currentRadiusAddition, radiusAddition)
        })

    override val descriptionVisibility : Observable<Boolean> =
        Observables.combineLatest(sunViewModel
        .outputs.isLoading, sunViewModel.outputs.isSunVisible) { isLoading, isSunVisible ->
            isLoading || !isSunVisible
        }

    override val descriptionTxt : Observable<String> =
        Observables.combineLatest(sunViewModel
            .outputs.isLoading, sunViewModel.outputs.isSunVisible) { isLoading, isSunVisible ->
            var txt = ""
            if (isLoading) {
                txt = "Looking for your location..."
            } else if (!isSunVisible) {
                txt = "The sun is currently not visible"
            }
            txt
        }


    override fun onResume() {
        compassViewModel.inputs.registerSensors()
    }

    override fun onPause() {
        compassViewModel.inputs.unregisterSensors()
    }

    override fun loadData() {
        persistence.saveLastDateLoadedSunPosition(Date())
        sunViewModel.inputs
            .loadSunPosition
            .onNext(0)
    }

    override fun loadDataIfNeeded() {
        val now = Date()
        val lastTime = persistence.getLastDateLoadedSunPosition()
        if (lastTime != null) {
            val diff = now.time - lastTime.time
            if (now.time - lastTime.time > thresholdToLoadDataAgain) {
                loadData()
            }
        }
    }

    private fun radiusAdditionFromDegree(degree: Float): Int {
        val value: Float = when (degree) {
            in 0f..90f -> 90f - degree
            in 90f..180f -> degree - 90f
            in 180f..270f -> (270f - degree).absoluteValue
            in 270f..360f -> (degree - 270f)
            else -> 0f
        }

        return (value / 1.5).toInt()
    }
}