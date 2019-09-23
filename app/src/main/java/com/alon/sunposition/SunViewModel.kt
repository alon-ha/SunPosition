package com.alon.sunposition

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
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
    val loadSunPosition: PublishSubject<Pair<Int, Coordinate>>
}

interface SunViewModelingOutputs {
    val sunPosition: Observable<SunPosition>
    val isLoading: Observable<Boolean>
}


class SunViewModel: SunViewModeling,
    SunViewModelingInputs, SunViewModelingOutputs {

    override val inputs = this
    override val outputs = this

    override val loadSunPosition = PublishSubject.create<Pair<Int, Coordinate>>()
    private val _sunPosition = BehaviorSubject.create<SunPosition>()
    override val sunPosition: Observable<SunPosition> = _sunPosition.hide()
    private val _isLoading = BehaviorSubject.create<Boolean>()
    override val isLoading: Observable<Boolean> = _isLoading.hide()

    private val networkAPI: NetworkingAPI = NetworkAPI()


    init {
        setupObservers()
    }

    private fun setupObservers() {
        loadSunPosition
            .subscribeOn(Schedulers.io())
            .doOnNext { _ ->
                _isLoading.onNext(true)
            }
            .flatMap { input ->
                val date = Date()
                networkAPI.loadSunPosition(date, input.second)
            }
            .doOnNext { sunPos ->
                _isLoading.onNext(false)
                _sunPosition.onNext(sunPos)
            }


    }
}