package com.alon.sunposition

import android.content.Context
import io.reactivex.*
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.functions.BiFunction
import java.util.*


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
    val sunAzimuthScreenRelative: Observable<AnimationData>
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

    override val sunAzimuthScreenRelative : Observable<AnimationData> = compassDegree
        .withLatestFrom(sunAzimuth)
        .map { (compassDegree, sunAzimuth) ->
            var relativeAzimuth = (sunAzimuth + compassDegree.toDouble()) % 360
            if (relativeAzimuth < 0) {
                relativeAzimuth += 360
            }
            relativeAzimuth
        }
        .map { degree -> degree.toFloat() }
        .scan( AnimationData(0f,0f), {animationData: AnimationData, degree: Float ->
            AnimationData(animationData.currentDegree, degree)
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
}