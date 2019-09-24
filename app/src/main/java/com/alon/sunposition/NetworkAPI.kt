package com.alon.sunposition

import io.reactivex.Observable
import org.shredzone.commons.suncalc.SunPosition
import java.util.Date

interface NetworkingAPI {
    fun loadSunPosition(date: Date, coordinate: Coordinate): Observable<SunPosition>
}

class NetworkAPI: NetworkingAPI {
    override fun loadSunPosition(date: Date, coordinate: Coordinate): Observable<SunPosition> {
        return Observable.create<SunPosition> { emitter ->
            val pos = SunPosition.compute().on(date).at(coordinate.latitude, coordinate.longitude).execute()
            emitter.onNext(pos)
            emitter.onComplete()
        }
    }
}