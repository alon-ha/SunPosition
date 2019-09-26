package com.alon.sunposition

import android.content.Context
import com.patloew.rxlocation.RxLocation
import io.reactivex.Observable

interface LocationServicing {
    fun fetchCurrentLocation(): Observable<Coordinate>
}

class LocationService(context: Context): LocationServicing {

    private val rxLocation = RxLocation(context)

    override fun fetchCurrentLocation(): Observable<Coordinate> {
        return rxLocation.location().lastLocation()
            .toObservable()
            .map { location ->
                Coordinate(location.latitude, location.longitude)
            }

    }
}