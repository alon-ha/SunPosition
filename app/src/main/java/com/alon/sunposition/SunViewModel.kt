package com.alon.sunposition

import android.content.Context
import android.util.Log
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import org.shredzone.commons.suncalc.SunPosition
import java.util.*


interface SunViewModeling {
    val outputs: SunViewModelingOutputs
    val inputs: SunViewModelingInputs

}

interface SunViewModelingInputs {
    val loadSunPosition: PublishSubject<Int>
}

interface SunViewModelingOutputs {
    val sunPosition: Observable<SunPosition>
    val isLoading: Observable<Boolean>
    val isSunVisible: Observable<Boolean>
}


class SunViewModel(context: Context): SunViewModeling,
    SunViewModelingInputs, SunViewModelingOutputs {

    override val inputs = this
    override val outputs = this

    override val loadSunPosition = PublishSubject.create<Int>()
    private val _sunPosition = BehaviorSubject.create<SunPosition>()
    override val sunPosition: Observable<SunPosition> = _sunPosition.hide()
    private val _isLoading = BehaviorSubject.create<Boolean>()
    override val isLoading: Observable<Boolean> = _isLoading.hide()
    private val _isSunVisible = BehaviorSubject.create<Boolean>()
    override val isSunVisible: Observable<Boolean> = _isSunVisible.hide()

    private val sunPositionService: SunPositionServicing = SunPositionService()
    private val locationService: LocationServicing = LocationService(context)

    init {
        setupObservers()
    }

    private fun setupObservers() {
        loadSunPosition
            .subscribeOn(Schedulers.io())
            .doOnNext {
                _isLoading.onNext(true)
                Log.d("Alon", "Before location--------")
            }
            .flatMap { hourOffset ->
                locationService.fetchCurrentLocation()
                    .map { Pair(hourOffset, it) }
            }
            .flatMap { input ->
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.HOUR_OF_DAY, input.first)
                val date = calendar.time
                sunPositionService.fetchSunPosition(date, input.second)
            }
            .doOnNext { sunPos ->
                _isLoading.onNext(false)
                val isVisible = sunPos.altitude > 0.0
                _isSunVisible.onNext(isVisible)
                _sunPosition.onNext(sunPos)
            }
            .subscribe({}, { error ->
                Log.d("Alon", "Error: " + error.localizedMessage)
            })
    }
}