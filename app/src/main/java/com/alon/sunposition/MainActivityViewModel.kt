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


class MainActivityViewModel: MainActivityViewModeling,
    MainActivityViewModelingInputs, MainActivityViewModelingOutputs {

    override val inputs = this
    override val outputs = this

    override lateinit var compassViewModel: CompassViewModeling
    override val sunViewModel: SunViewModeling = SunViewModel()

    private val compassDegree: Observable<Float> = compassViewModel.outputs
        .compassPosition
        .map { pos -> pos.currentDegree }

    override val sunAzimuthScreenRelative : Observable<AnimationData> = sunViewModel
        .outputs.sunPosition
        .map { pos -> pos.azimuth }
        .withLatestFrom(compassDegree)
        .map { (sunAzimuth, compassDegree) ->
            (sunAzimuth + compassDegree.toDouble()) % 360
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


    constructor(context: Context) {
        compassViewModel = CompassViewModel(context)
    }


    override fun onResume() {
        compassViewModel.inputs.registerSensors()
    }

    override fun onPause() {
        compassViewModel.inputs.unregisterSensors()
    }

    override fun loadData() {
        val coordinate = Coordinate(0.0,0.0)

        sunViewModel.inputs
            .loadSunPosition
            .onNext(Pair(0, coordinate))
    }

}