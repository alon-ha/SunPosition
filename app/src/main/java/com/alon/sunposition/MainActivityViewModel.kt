package com.alon.sunposition

import android.content.Context
import io.reactivex.*


interface MainActivityViewModeling {
    val outputs: MainActivityViewModelingOutputs
    val inputs: MainActivityViewModelingInputs

}

interface MainActivityViewModelingInputs {
    fun onResume()
    fun onPause()
}

interface MainActivityViewModelingOutputs {
    val compassViewModel: CompassViewModeling
    val sunViewModel: SunViewModeling
}


class MainActivityViewModel: MainActivityViewModeling,
    MainActivityViewModelingInputs, MainActivityViewModelingOutputs {

    override val inputs = this
    override val outputs = this

    override val compassViewModel: CompassViewModeling
    override val sunViewModel: SunViewModeling = SunViewModel()

    constructor(context: Context) {
        compassViewModel = CompassViewModel(context)
    }


    override fun onResume() {
        compassViewModel.inputs.registerSensors()
    }

    override fun onPause() {
        compassViewModel.inputs.unregisterSensors()
    }
}