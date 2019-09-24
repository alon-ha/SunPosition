package com.alon.sunposition

import io.reactivex.Observable
import org.shredzone.commons.suncalc.SunPosition
import java.util.Date

interface SunPositionServicing {
    fun fetchSunPosition(date: Date, coordinate: Coordinate): Observable<SunPosition>
}

class SunPositionService: SunPositionServicing {
    override fun fetchSunPosition(date: Date, coordinate: Coordinate): Observable<SunPosition> {
        return Observable.create<SunPosition> { emitter ->
            val pos = SunPosition.compute().on(date).at(coordinate.latitude, coordinate.longitude).execute()
            emitter.onNext(pos)
            emitter.onComplete()
        }
    }
}