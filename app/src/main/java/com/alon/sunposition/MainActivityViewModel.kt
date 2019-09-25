package com.alon.sunposition

import android.content.Context
import io.reactivex.*
import io.reactivex.Observable
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.withLatestFrom
import io.reactivex.functions.BiFunction


interface MainActivityViewModeling {
    val outputs: MainActivityViewModelingOutputs
    val inputs: MainActivityViewModelingInputs

}

interface MainActivityViewModelingInputs {
    fun onResume()
    fun onPause()
    fun loadData()
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
        sunViewModel.inputs
            .loadSunPosition
            .onNext(0)
    }
}