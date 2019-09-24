package com.alon.sunposition

import android.Manifest
import android.content.Context
import com.patloew.rxlocation.RxLocation
import io.reactivex.Observable
import com.google.android.gms.location.LocationRequest
import android.Manifest.permission
import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat





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